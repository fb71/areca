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
package areca.aws.ec2proxy;

import static java.time.Instant.now;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import areca.aws.AWS;
import areca.aws.XLogger;

/**
 *
 *
 * @author Falko Bräutigam
 */
public class VHost {

    private static final XLogger LOG = XLogger.get( VHost.class );

    private static final Timer timer = new Timer();

    /**
     *
     */
    public static List<VHost> readConfig( AWS aws ) {
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
            return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
                    .fromJson( in, ConfigFile.class ).vhosts;
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

    AtomicBoolean           isRunning;

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
    @SerializedName("indexRedirect")
    public String           indexRedirect;

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


    protected void init( AWS aws ) {
        if (ec2id != null) {
            isRunning = new AtomicBoolean( aws.isInstanceRunning( ec2id ) );
            touch();

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
            var interval = Duration.ofSeconds( 60 ).toMillis();
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


    public VHost touch() {
        lastAccess = Instant.now();
        return this;
    }


    public void updateRunning( Boolean expected, Callable<Boolean> block ) throws Exception {
        if (expected == null || expected.booleanValue() == isRunning.get() ) {
            synchronized (isRunning) {
                if (expected == null || expected.booleanValue() == isRunning.get() ) {
                    isRunning.set( block.call() );
                }
            }
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