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
package areca.common.base.log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Falko Bräutigam
 */
public class LogFactory {

    public static Log getLog( Class<?> cl ) {
        return new Log( cl );
    }

    /**
     *
     */
    public static class Log {

        protected Logger        delegate;

        public Log( Class<?> cl ) {
            delegate = Logger.getLogger( cl.getName() );
        }

        public void warning( String msg ) {
            delegate.warning( msg );
        }

        public void warn( String msg ) {
            delegate.warning( msg );
        }

        public void warn( String msg, Throwable e ) {
            delegate.log( Level.WARNING, msg, e );
        }

        public void info( String msg ) {
            delegate.info( msg );
        }

        public void debug( String msg ) {
            delegate.log( Level.FINE, msg );
        }
    }
}
