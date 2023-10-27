/*
 * Copyright (C) 2023, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package areca.aws;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.FileReader;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 *
 *
 * @author Falko Bräutigam
 */
public class VHost {

    private static final XLogger LOG = XLogger.get( VHost.class );

    public static final AWS aws = new AWS();

    //private static final Timer timer = new Timer();

    public static List<VHost> readConfig() {
        try (var in = new FileReader( "/home/falko/workspaces/workspace-android/areca/areca.aws/src/test/resources/config.json" ) ) {
            var vhosts = new Gson().fromJson( in, ConfigFile.class ).vhosts;
            vhosts.forEach( vhost -> vhost.init() );
            return vhosts;
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }

    public class ConfigFile {

        public List<VHost>  vhosts;
    }


    // instance *******************************************

    private AtomicBoolean   isRunning;

    private volatile int    pending; // XXX

    private long            lastAccess;

    /** The URL server names of the virtual server */
    @SerializedName("hostnames")
    public List<String>     hostnames;

    /** The EC2 instance id */
    @SerializedName("ec2id")
    public String           ec2id;

    @SerializedName("proxypaths")
    public List<ProxyPath>  proxypaths;

    public class ProxyPath {

        @SerializedName("path")
        public String path;

        @SerializedName("redirect")
        public String redirect;
    }


    protected void init() {
        if (ec2id != null) {
            isRunning = new AtomicBoolean( aws.isInstanceRunning( ec2id ) );
            lastAccess = System.currentTimeMillis();
        }
    }

    public void ensureRunning( Callable<?> block ) throws Exception {
        lastAccess = System.currentTimeMillis();

        var wasRunning = isRunning.get();
        if (!isRunning.get()) {
            synchronized (isRunning) {
                if (!isRunning.get()) {
                    aws.startInstance( ec2id );
                    isRunning.set( true );
                    Thread.sleep( 1000 ); // allow services to start
                }
            }
        }
        try {
            block.call();
        }
        // we expect the instance to run but no connect
        catch (HttpConnectTimeoutException|ConnectException e) {
            LOG.info( "No connection: %s/%s (wasRunning=%s)", hostnames.get( 0 ), ec2id, wasRunning );
            synchronized (isRunning) {
                aws.stopInstance( ec2id );
                aws.startInstance( ec2id );
                isRunning.set( true );
                Thread.sleep( 1000 ); // allow services to start
            }
            block.call();
        }
    }


    // Test ***********************************************

    public static void main( String[] args ) throws Exception {
        var config = new Gson().fromJson( new FileReader( "src/test/resources/config.json" ), ConfigFile.class );

        for (VHost vhost : config.vhosts) {
            System.out.println( "vhost: " + vhost.hostnames );
        }
    }

}