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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class ProgressMonitor {

    private static final Log LOG = LogFactory.getLog( ProgressMonitor.class );

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

    public abstract ProgressMonitor beginTask( String name, int totalWork );

    public abstract ProgressMonitor subTask( String name );

    public abstract ProgressMonitor worked( int work );

    public abstract ProgressMonitor done();

    public ProgressMonitor subMonitor() {
        return new SubMonitor( this );
    }

    public abstract void updateTotalWork( int toAdd );



    /**
     *
     */
    public static class SubMonitor
            extends ProgressMonitor {

        protected ProgressMonitor   parent;

        protected int               workTotal;

        protected int               workDone;

        protected String            taskName;


        public SubMonitor( ProgressMonitor parent ) {
            this.parent = parent;
        }

        @Override
        public ProgressMonitor beginTask( String name, int totalWork ) {
            LOG.debug( "SUB: beginTask: %s / %s ", name, totalWork );
            this.taskName = name;
            this.workTotal = totalWork;

            parent.subTask( name );
            parent.updateTotalWork( totalWork );
            return this;
        }

        @Override
        public ProgressMonitor subTask( String name ) {
            parent.subTask( String.format( "%s - %s", taskName, name ) );
            return this;
        }

        @Override
        public ProgressMonitor worked( int work ) {
            workDone += work;
            parent.worked( work );
            return this;
        }

        @Override
        public ProgressMonitor done() {
            int workLeft = Math.max( 0, workTotal - workDone );
            parent.worked( workLeft );
            workDone = workTotal;
            LOG.debug( "SUB: done: %s / %s ", workLeft, workTotal );
            return this;
        }

        @Override
        public void updateTotalWork( int toAdd ) {
            parent.updateTotalWork( toAdd );
        }

    }


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
