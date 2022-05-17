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

/**
 *
 * @author Falko Bräutigam
 */
public abstract class ProgressMonitor {

    public static final int UNKNOWN = 0;

    protected volatile boolean  cancelled;

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void checkCancelled() {
        if (isCancelled()) {
            throw new CancelledException( "Operation was cancelled.", null );
        }
    }

    public abstract void beginTask( String name, int totalWork );

    public abstract void subTask( String name );

    public abstract void worked( int work );

    public abstract void done();


    /**
     *
     */
    public static class CancelledException
            extends RuntimeException {

        protected CancelledException( String message, Throwable cause ) {
            super( message, cause );
        }

    }
}
