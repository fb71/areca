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

import static areca.aws.HttpForwardServlet3.TIMEOUT_SERVICES_STARTUP;
import static java.time.Instant.now;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.File;
import java.io.FileReader;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 *
 *
 * @author Falko Bräutigam
 */
public class VHost {

    private static final XLogger LOG = XLogger.get( VHost.class );

    public static final AWS aws = new AWS();

    private static final Timer timer = new Timer();

    public static List<VHost> readConfig() {
        var home = System.getProperty( "user.home");
        var f = new File( home, "proxy.config" );
        if (!f.exists()) {
            LOG.info( "No config at: %s", f.getAbsolutePath() );
            f = new File( home, "workspaces/workspace-android/areca/areca.aws/src/test/resources/config.json" );
        }
        else if (!f.exists()) {
            throw new RuntimeException( "No config: " + new File( home, "proxy.config" ).getAbsolutePath() );
        }

        try (var in = new FileReader( f ) ) {
            var vhosts = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
                    .fromJson( in, ConfigFile.class ).vhosts;
            vhosts.forEach( vhost -> vhost.init() );
            return vhosts;
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }

    public class ConfigFile {

        @Expose
        public List<VHost>  vhosts;
    }


    // instance *******************************************

    private AtomicBoolean   isRunning;

    private volatile int    pending; // XXX

    private volatile Instant lastAccess;

    private TimerTask       idleCheck;

    /** The URL server names of the virtual server */
    @Expose
    @SerializedName("hostnames")
    public List<String>     hostnames;

    /** The EC2 instance id */
    @Expose
    @SerializedName("ec2id")
    public String           ec2id;

    @Expose
    @SerializedName("idleTimeout")
    public String           idleTimeout;

    @Expose
    @SerializedName("proxypaths")
    public List<ProxyPath>  proxypaths;

    public class ProxyPath {

        @Expose
        @SerializedName("path")
        public String path;

        @Expose
        @SerializedName("redirect")
        public String redirect;
    }


    protected void init() {
        if (ec2id != null) {
            isRunning = new AtomicBoolean( aws.isInstanceRunning( ec2id ) );
            lastAccess = Instant.now();

            // idle check
            var idle = Duration.parse( idleTimeout );
            idleCheck = new TimerTask() {
                @Override public void run() {
                    LOG.info( "Idle check: %s/%s, last: %s", hostnames.get( 0 ), ec2id, lastAccess );
                    if (isRunning.get() && Duration.between( lastAccess, now() ).compareTo( idle ) > 0) {
                        LOG.info( "IDLE: %s/%s, stopping...", hostnames.get( 0 ), ec2id );
                        synchronized (isRunning) {
                            if (isRunning.get()) {
                                aws.stopInstance( ec2id );
                                isRunning.set( false );
                            }
                        }
                    }
                }
            };
            var interval = Duration.ofSeconds( 10 ).toMillis();
            timer.scheduleAtFixedRate( idleCheck, interval, interval );
            LOG.info( "Idle check started: %s/%s, at interval: %s", hostnames.get( 0 ), ec2id, idle );
        }
    }


    public void dispose() {
        if (idleCheck != null) {
            idleCheck.cancel();
            idleCheck = null;
        }
    }


    public void ensureRunning( Callable<?> block ) throws Exception {
        lastAccess = Instant.now();

        var wasRunning = isRunning.get();

        // expect not running
        if (!isRunning.get()) {
            synchronized (isRunning) {
                if (!isRunning.get()) {
                    aws.startInstance( ec2id );
                }
                tryExecute( block );
                isRunning.set( true );
            }
        }

        // expect running
        else {
            try {
                block.call();
            }
            // we expect the instance to run but no connect
            catch (HttpTimeoutException|ConnectException e) {
                LOG.info( "No connection: %s/%s (wasRunning=%s) - %s", hostnames.get( 0 ), ec2id, wasRunning, e );
                synchronized (isRunning) {
                    // instance is down or service is down or crashed
                    //aws.stopInstance( ec2id );
                    isRunning.set( false );
                    aws.startInstance( ec2id );

                    tryExecute( block );

                    isRunning.set( true );
                }
            }
        }
    }


    protected void tryExecute( Callable block ) throws Exception {
        for (var start = Instant.now(); Duration.between( start, Instant.now() ).compareTo( TIMEOUT_SERVICES_STARTUP ) < 0;) {
            try {
                block.call();
                return;
            }
            catch (HttpTimeoutException|ConnectException e) {
                LOG.info( "Waiting for connection: %s/%s", hostnames.get( 0 ), ec2id );
                Thread.sleep( 1000 );
            }
        }
        throw new HttpConnectTimeoutException( "No connection after: " + TIMEOUT_SERVICES_STARTUP.toSeconds() + "s" );
    }


    // Test ***********************************************

    public static void main( String[] args ) throws Exception {
        var config = new Gson().fromJson( new FileReader( "src/test/resources/config.json" ), ConfigFile.class );

        for (VHost vhost : config.vhosts) {
            System.out.println( "vhost: " + vhost.hostnames );
        }
    }

}