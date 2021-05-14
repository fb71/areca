/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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

import java.util.concurrent.Callable;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class Platform {

    public static PlatformImpl impl;


    public static <R> Promise<R> schedule( int delayMillis, Callable<R> task ) {
        return impl.schedule( delayMillis, task );
    }


    public static void schedule( int delayMillis, Runnable task ) {
        schedule( delayMillis, () -> {
            task.run();
            return null;
        });
    }


    public static void async( Runnable task ) {
        schedule( 0, task );
    }


    public static <R> Promise<R> async( Callable<R> task ) {
        return schedule( 0, task );
    }


    public static Throwable rootCause( Throwable e ) {
            Throwable cause = e;
            while (cause.getCause() != null && cause.getCause() != cause) {
                cause = cause.getCause();
            }
            return cause;

    //        for (var cause = e;; cause = cause.getCause()) {
    //            if (cause.getCause() == null || cause.getCause() != cause) {
    //                return cause;
    //            }
    //        }
    //
    //        Sequence.series( e,
    //                cause -> cause.getCause(),
    //                cause -> cause.getCause() != null && cause.getCause() != cause )
    //                .last();
        }


    /**
     *
     */
    public interface PlatformImpl {
        public <R> Promise<R> schedule( int delayMillis, Callable<R> task );

    }
}
