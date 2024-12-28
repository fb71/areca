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
package areca.aws;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.substring;

import java.util.logging.Level;
import java.util.logging.Logger;

import areca.aws.ec2proxy.ConfigFile;

/**
 *
 * @author Falko Bräutigam
 */
@SuppressWarnings("rawtypes")
public class XLogger {

    public static Level LEVEL_DEFAULT = Level.INFO;

    public static void init( ConfigFile config ) {
        LEVEL_DEFAULT = Level.parse( config.log.level );
        System.err.println( XLogger.class.getName() + "           : " + LEVEL_DEFAULT );
    }

    public static XLogger get( Class cl ) {
        return new XLogger( cl, null ); //Logger.getLogger( cl.getName() ) );
    }

    // instance *******************************************

    private Logger  delegate;

    private Class   cl;

    protected XLogger( Class cl, Logger logger ) {
        this.delegate = logger;
        this.cl = cl;
    }

    public Logger delegate() {
        return delegate;
    }

    @SuppressWarnings( "resource" )
    protected XLogger log( Level level, String format, Object... args ) {
        if (delegate != null) {
            if (delegate.isLoggable( level )) {
                var formatted = args != null ? String.format( format, args ) : format;
                delegate.log( level, formatted );
            }
        }
        else {
            if (level.intValue() >= LEVEL_DEFAULT.intValue()) {
                var formatted = args != null ? String.format( format, args ) : format;
                var prefix = abbreviate( cl.getSimpleName(), 20 );
                var l = substring( level.toString(), 0, 5);
                var out = level.intValue() >= Level.WARNING.intValue() ? System.err : System.out;
                out.println( String.format( "[%-5s] %-20s: %s", l, prefix, formatted ) );
            }
        }
        return this;
    }

    public XLogger debug( String format, Object... args ) {
        return log( Level.FINE, format, args );
    }

    public XLogger info( String format, Object... args ) {
        return log( Level.INFO, format, args );
    }

    public XLogger warn( String format, Object... args ) {
        return log( Level.WARNING, format, args );
    }

}
