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

import static areca.aws.ec2proxy.HttpForwardServlet4.LOG_INSTANCE;
import static java.time.Instant.now;

import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.function.FailableFunction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import areca.aws.AWS;
import areca.aws.XLogger;
import areca.aws.logs.Ec2InstanceEvent;

/**
 *
 *
 * @author Falko Bräutigam
 */
public class VHost {

    private static final XLogger LOG = XLogger.get( VHost.class );

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
            return new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation().create()
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

    AtomicBoolean           isRunning = new AtomicBoolean();

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

        @Expose
        @SerializedName("forward")
        public String forward;
    }


    protected void init( AWS aws ) {
        if (ec2id != null) {
            isRunning.set( aws.isInstanceRunning( ec2id ) );
            touch();

            // idle check
            var idle = Duration.parse( idleTimeout );
            idleCheck = new TimerTask() {
                @Override public void run() {
                    // LOG.debug( "Idle check: %s/%s, last: %s", hostnames.get( 0 ), ec2id, lastAccess );
                    if (isRunning.get() && Duration.between( lastAccess, now() ).compareTo( idle ) > 0) {
                        LOG.info( "IDLE: %s/%s, stopping...", hostnames.get( 0 ), ec2id );
                        updateRunning( null, __ -> {
                            aws.stopInstance( ec2id );
                            return false;
                        });
                    }
                }
            };
            var interval = Duration.ofSeconds( 60 ).toMillis();
            HttpForwardServlet4.timer.scheduleAtFixedRate( idleCheck, interval, interval );
            LOG.info( "Idle check started: %s/%s, at interval: %s", hostnames.get( 0 ), ec2id, idle );
        }
        else {
            LOG.info( "No ec2id for: %s. Assuming running target host", hostnames.get( 0 ) );
            isRunning.set( true );
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


    public <E extends Exception> void updateRunning( Boolean expected, FailableFunction<Boolean,Boolean,E> block ) throws E {
        assert ec2id != null;
        if (expected == null || expected.booleanValue() == isRunning.get() ) {
            synchronized (isRunning) {
                if (expected == null || expected.booleanValue() == isRunning.get() ) {
                    var before = isRunning.get();
                    isRunning.set( block.apply( isRunning.get() ) );

                    HttpForwardServlet4.logs.publish( LOG_INSTANCE, new Ec2InstanceEvent( this, before, isRunning.get() ) );
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