/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.common;

import java.util.concurrent.TimeUnit;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class Timer {

    private static final Log log = LogFactory.getLog( Timer.class );

    public static Timer start() {
        return new Timer();
    }

    // instance *******************************************

    private long        start = System.nanoTime();


    public Timer restart() {
        start = System.nanoTime();
        return this;
    }


    public long elapsedNanos() {
        return System.nanoTime() - start;
    }


    public long elapsed( TimeUnit unit ) {
        return unit.convert( elapsedNanos(), TimeUnit.NANOSECONDS );
    }


    public String elapsedHumanReadable() {
        long millis = elapsed( TimeUnit.MILLISECONDS );
        if (millis < 1000) {
            return millis + "ms";
        }
        long seconds = elapsed( TimeUnit.SECONDS );
        if (seconds < 600) {
            return new StringBuilder( 16 ).append( seconds ).append( "." ).append( millis ).append( "sec" ).toString();
        }
        throw new RuntimeException( "Time exceeds range: " + elapsed( TimeUnit.MINUTES ) + "+ minutes" );
    }
}
