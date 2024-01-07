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

import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

import areca.common.base.Sequence;
import areca.common.base.Supplier.RSupplier;

/**
 *
 * @author Falko Bräutigam
 */
public class LogFactory {

    public enum Level {
        DEBUG, INFO, WARN, ERROR, OFF,
        /** Signals that the logger should use the current {@link LogFactory#DEFAULT_LEVEL}. */
        DEFAULT
    }

    /** The level that is used for loggers with current level {@link Level#DEFAULT}. */
    public static Level DEFAULT_LEVEL = Level.INFO;

    private static Map<String,Level> levels = new HashMap<>();

    public static Log getLog( Class<?> cl ) {
        return new Log( cl, null );
    }

    public static Log getLog( String prefix, Class<?> cl ) {
        return new Log( cl, prefix );
    }

    public static void setClassLevel( Class<?> cl, Level level ) {
        System.out.println( "LOG: " + cl.getName() + " -> " + level );
        levels.put( cl.getName(), level );
    }

    public static void setPackageLevel( Class<?> cl, Level level ) {
        String packageName = StringUtils.substringBeforeLast( cl.getName(), "." );
        System.out.println( "LOG: " + packageName + " -> " + level );
        levels.put( packageName, level );
    }

    public static Object[] a( Object... args ) {
        return args;
    }

    /**
     *
     */
    public static class Log {

        protected final String  prefix;

        protected final Level   level;


        public Log( Class<?> cl, String prefix ) {
            this.prefix = prefix != null ? prefix : cl.getSimpleName();

            this.level = Sequence.of( levels.entrySet() )
                    .filter( entry -> cl.getName().startsWith( entry.getKey() ) )
                    .reduce( (e1,e2) -> e1.getKey().length() > e2.getKey().length() ? e1 : e2 ) // FIXME does not seem to work correctly
                    .map( entry -> entry.getValue() )
                    .orElse( Level.DEFAULT );
        }

        public String format( Level msgLevel, String msg, Object... args ) {
            var formatted = args != null ? String.format( msg, args ) : msg;
            return String.format( "[%-5s] %-20s: %s", msgLevel, abbreviate(prefix,20), formatted );
        }

        private void doLog( Level msgLevel, String msg, Object[] args, Throwable e ) {
            @SuppressWarnings("resource")
            var out = msgLevel.ordinal() >= Level.WARN.ordinal() ? System.err : System.out;
            out.println( format( msgLevel, msg, args ) );
        }

        protected void log( Level msgLevel, String msg, Object[] args, Throwable e ) {
            if (isLevelEnabled( msgLevel )) {
                //Platform.scheduler.schedule( Priority.DECORATION, () -> {
                    doLog( msgLevel, msg, args, e );
                //});
            }
        }

        protected void log2( Level msgLevel, String msg, RSupplier<Object[]> args, Throwable e ) {
            if (isLevelEnabled( msgLevel )) {
                //Platform.scheduler.schedule( Priority.DECORATION, () -> {
                    doLog( msgLevel, msg, args.get(), e );
                //});
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

        public final void info( String format, RSupplier<Object[]> args ) {
            log2( Level.INFO, format, args, null );
        }

        public void debug( String format, Object... args ) {
            log( Level.DEBUG, format, args, null );
        }

        public final void debug( String format, RSupplier<Object[]> args ) {
            log2( Level.DEBUG, format, args, null );
        }

        public boolean isLevelEnabled( Level l ) {
            var currentLevel = level != Level.DEFAULT ? level : DEFAULT_LEVEL;
            return l.ordinal() >= currentLevel.ordinal();
        }
    }
}
