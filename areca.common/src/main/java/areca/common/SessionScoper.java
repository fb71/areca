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
package areca.common;

import areca.common.base.Consumer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class SessionScoper {

    private static final Log LOG = LogFactory.getLog( SessionScoper.class );

    static SessionScoper instance;

    public static void setInstance( SessionScoper scoper ) {
        Assert.isNull( instance );
        instance = Assert.notNull( scoper );
    }

    // instance *******************************************

    public abstract Session currentSession();

    /**
     * One global {@link Session} for the entire JVM.
     */
    public static class JvmSessionScoper
            extends SessionScoper {

        private Session global = new Session();

        @Override
        public Session currentSession() {
            return global;
        }
    }

    /**
     *
     */
    public static class ThreadBoundSessionScoper
            extends SessionScoper {

        private static ThreadLocal<Session> sessions = new ThreadLocal<>();

        public void bind( Session session ) {
            Assert.isNull( sessions.get() );
            sessions.set( Assert.notNull( session ) );
        }

        public void unbind( Session session ) {
            Assert.isEqual( session, sessions.get() );
            sessions.remove();
        }

        public <E extends Exception> void bind( Session session, Consumer<Session,E> consumer ) throws E {
            try {
                bind( session );
                consumer.accept( session );
            }
            finally {
                unbind( session );
            }
        }

        @Override
        public Session currentSession() {
            return Assert.notNull( sessions.get(), "No Session for the current thread." );
        }
    }
}
