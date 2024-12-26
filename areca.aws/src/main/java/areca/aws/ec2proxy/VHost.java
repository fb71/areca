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
import java.util.stream.Collectors;

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.function.FailableFunction;

import areca.aws.AWS;
import areca.aws.XLogger;
import areca.aws.ec2proxy.ConfigFile.VHostConfig;
import areca.aws.ec2proxy.ConfigFile.VHostConfig.ProxyPath;
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
    public static List<VHost> readConfig( ConfigFile config, AWS aws ) {
        return config.vhosts.stream().map( c -> new VHost( c, aws ) ).collect( Collectors.toList() );
    }

    // instance *******************************************

    AtomicBoolean           isRunning = new AtomicBoolean();

    private volatile Instant lastAccess;

    private TimerTask       idleCheck;

    private VHostConfig     config;


    protected VHost( VHostConfig config, AWS aws ) {
        this.config = config;
        if (config.ec2id != null) {
            isRunning.set( aws.isInstanceRunning( config.ec2id ) );
            touch();

            // idle check
            var idle = Duration.parse( config.idleTimeout );
            idleCheck = new TimerTask() {
                @Override public void run() {
                    // LOG.debug( "Idle check: %s/%s, last: %s", hostnames.get( 0 ), ec2id, lastAccess );
                    if (isRunning.get() && Duration.between( lastAccess, now() ).compareTo( idle ) > 0) {
                        LOG.info( "IDLE: %s/%s, stopping...", config.hostnames.get( 0 ), config.ec2id );
                        updateRunning( null, __ -> {
                            aws.stopInstance( config.ec2id );
                            return false;
                        });
                    }
                }
            };
            var interval = Duration.ofSeconds( 60 ).toMillis();
            HttpForwardServlet4.timer.scheduleAtFixedRate( idleCheck, interval, interval );
            LOG.info( "Idle check started: %s/%s, at interval: %s", config.hostnames.get( 0 ), config.ec2id, idle );
        }
        else {
            LOG.info( "No ec2id for: %s. Assuming running target host", config.hostnames.get( 0 ) );
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
        assert config.ec2id != null;
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


    public List<String> hostnames() {
        return config.hostnames;
    }


    public List<ProxyPath> proxypaths() {
        return config.proxypaths;
    }


    public String ec2id() {
        return config.ec2id;
    }


    public String robots() {
        return config.robots;
    }

}