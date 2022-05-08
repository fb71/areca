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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

import areca.common.base.Sequence;

/**
 *
 * @author Falko Bräutigam
 */
public class LogFactory {

    public enum Level {
        DEBUG, INFO, WARN, ERROR, OFF
    }

    public static Level DEFAULT_LEVEL = Level.INFO;

    public static Log getLog( Class<?> cl ) {
        init();
        return new Log( cl, null );
    }

    public static Log getLog( String prefix, Class<?> cl ) {
        init();
        return new Log( cl, prefix );
    }

    public static void setClassLevel( Class<?> cl, Level level ) {
        init();
        System.out.println( "LOG: " + cl.getName() + " -> " + level );
        levels.put( cl.getName(), level );
    }

    public static void setPackageLevel( Class<?> cl, Level level ) {
        init();
        String packageName = StringUtils.substringBeforeLast( cl.getName(), "." );
        System.out.println( "LOG: " + packageName + " -> " + level );
        levels.put( packageName, level );
    }

    public static void init() {
        if (levels == null) {
            levels = new HashMap<>();
        }
    }

    private static Map<String,Level> levels;


    /**
     *
     */
    public static class Log {

        protected Class<?>                      cl;

        protected String                        prefix;

        protected Level                         level;


        public Log( Class<?> cl, String prefix ) {
            this.cl = cl;
            this.prefix = prefix != null ? prefix : cl.getSimpleName();

            this.level = Sequence.of( levels.entrySet() )
                    .filter( entry -> cl.getName().startsWith( entry.getKey() ) )
                    .reduce( (e1,e2) -> e1.getKey().length() > e2.getKey().length() ? e1 : e2 )
                    .ifPresentMap( entry -> entry.getValue() )
                    .orElse( DEFAULT_LEVEL );
        }

        protected void log ( Level msgLevel, String msg, Object[] args, Throwable e ) {
            if (msgLevel.ordinal() >= level.ordinal()) {
                var formatted = args != null ? String.format( msg, args ) : msg;
                var record = String.format( "[%-5s] %s: %s", msgLevel, prefix, formatted );
                if (msgLevel.ordinal() >= Level.WARN.ordinal()) {
                    System.err.println( record );
                }
                else {
                    System.out.println( record );
                }
            }
        }

        public void warn( String msg ) {
            log( Level.WARN, msg, null, null );
        }

        public void warn( String msg, Throwable e ) {
            log( Level.WARN, msg, null, e );
        }

        public void info( String format, Object... args ) {
            log( Level.INFO, format, args, null );
        }

        public void debug( String format, Object... args ) {
            log( Level.DEBUG, format, args, null );
        }
    }
}
