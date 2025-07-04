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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import java.time.format.DateTimeFormatter;

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


    @Override
    public String toString() {
        return elapsedHumanReadable();
    }


    public Timer restart() {
        start = System.nanoTime();
        return this;
    }


    public long elapsedNanos() {
        return System.nanoTime() - start;
    }


    public long elapsedMillis() {
        return elapsed( TimeUnit.MILLISECONDS );
    }


    /**
     * The remaining time between {@link #start} or last {@link #restart()} if the
     * maximum available time id the given value.
     *
     * @return <code>Math.max( 0, max - elapsed( TimeUnit.MILLISECONDS ) )</code>
     */
    public int remainingMillis( int max ) {
        return (int)Math.max( 0, max - elapsed( TimeUnit.MILLISECONDS ) );
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
            return new StringBuilder( 16 )
                    .append( seconds ).append( "." )
                    .append( Math.round( (millis - (seconds*1000)) / 100 ) )
                    .append( "s" ).toString();
        }
        else {
            var formatter = DateTimeFormatter.ofPattern( "mmm:ss" ).toFormat();
            return formatter.format( new Date( elapsed( TimeUnit.SECONDS ) ) );
        }
    }
}
