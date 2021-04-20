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

import java.util.ArrayDeque;
import java.util.Deque;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class AsyncJob {

    private static final Log log = LogFactory.getLog( AsyncJob.class );

    @FunctionalInterface
    public static interface UnitOfWork<E extends Exception> {
        void run( Site site ) throws E;
    }

    /**
     *
     */
    public class Site {

        protected ProgressMonitor   subMonitor;

        public ProgressMonitor monitor() {
            return subMonitor;
        }
    }

    private enum Status {
        INITIALIZED, STARTED, DONE
    }

    // instance *******************************************

    private ProgressMonitor         monitor = new NullProgressMonitor();

    private Status                  status = Status.INITIALIZED;

    private Deque<UnitOfWork<?>>    workUnits = new ArrayDeque<>();


    public AsyncJob schedule( String task, UnitOfWork<?> work ) {
        return schedule( task, 1, work );
    }


    public AsyncJob schedule( String task, int amount, UnitOfWork<?> work ) {
        workUnits.addLast( work );
        return this;
    }


    public AsyncJob start() {
        Assert.isEqual( Status.INITIALIZED, status, "Wrong status: " + status );
        Assert.that( !workUnits.isEmpty(), "No work units have been added yet." );
        monitor.beginTask( "", workUnits.size() );
        status = Status.STARTED;
        next();
        return this;
    }


    protected void next() {
        Platform.instance().async( () -> {
            try {
                if (monitor.isCancelled()) {
                    status = Status.DONE;
                    return;
                }
                var workUnit = workUnits.pollFirst();
                if (workUnit != null) {
                    monitor.subTask( "???" );
                    workUnit.run( null );
                    monitor.worked( 1 );
                    next();
                }
                else {
                    status = Status.DONE;
                }
            }
            catch (Exception e) {
                throw new RuntimeException( e );
            }
        });
    }

}
