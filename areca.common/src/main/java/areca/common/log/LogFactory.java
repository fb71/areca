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
package areca.common.log;

import java.util.logging.Level;
import java.util.logging.Logger;

import areca.common.base.Lazy;

/**
 *
 * @author Falko Bräutigam
 */
public class LogFactory {

    public static Log getLog( Class<?> cl ) {
        return new Log( cl, null );
    }

    public static Log getLog( String prefix, Class<?> cl ) {
        return new Log( cl, prefix );
    }

    /**
     *
     */
    public static class Log {

        protected Class<?>                      cl;

        protected Lazy<Logger,RuntimeException> delegate;

        protected String                        prefix;


        public Log( Class<?> cl, String prefix ) {
            this.cl = cl;
            this.delegate = new Lazy<>( () -> Logger.getLogger( Log.this.cl.getName() ) );
            this.prefix = prefix;
        }

        protected String prefixed( String msg ) {
            return new StringBuilder( 64 )
                    .append( prefix != null ? prefix : cl.getSimpleName() ).append( ": " )
                    .append( msg ).toString();
        }

        public void warning( String msg ) {
            delegate.supply().warning( prefixed( msg ) );
        }

        public void warn( String msg ) {
            delegate.supply().warning( prefixed( msg ) );
        }

        public void warn( String msg, Throwable e ) {
            delegate.supply().log( Level.WARNING, prefixed( msg ), e );
        }

        public void info( String msg ) {
            delegate.supply().info( prefixed( msg ) );
        }

        public void info( String format, Object... args ) {
            delegate.supply().info( prefixed( String.format( format, args ) ) );
        }

        public void debug( String format, Object... args ) {
            delegate.supply().log( Level.FINE, prefixed( String.format( format, args ) ) );
        }
    }
}
