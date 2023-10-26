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

import com.google.gson.Gson;

/**
 *
 *
 * @author Falko Bräutigam
 */
public class VHost {

    public static final AWS aws = new AWS();

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

    /** The URL server names of the virtual server */
    public List<String>     hostnames;

    /** The EC2 instance id */
    public String           ec2id;

    private AtomicBoolean   isRunning;

    private volatile int    pending; // XXX

    private long            lastAccess;

    public List<ProxyPath>  proxypaths;

    public class ProxyPath {

        public String path;

        public String redirect;
    }

    protected void init() {
        if (ec2id != null) {
            var instance = aws.describeEC2Instances( ec2id );
            String state = instance.state().name().name();
            isRunning = new AtomicBoolean( state.compareTo( "RUNNING" ) == 0 );
            lastAccess = System.currentTimeMillis();
        }
    }

    public void ensureRunning( Callable<?> block ) throws Exception {
        if (!isRunning.get()) {
            synchronized (isRunning) {
                if (!isRunning.get()) {
                    aws.startInstance( ec2id );
                    isRunning.set( true );
                }
            }
        }
        block.call();
    }


    // Test ***********************************************

    public static void main( String[] args ) throws Exception {
        var config = new Gson().fromJson( new FileReader( "src/test/resources/config.json" ), ConfigFile.class );

        for (VHost vhost : config.vhosts) {
            System.out.println( "vhost: " + vhost.hostnames );
        }
    }

}