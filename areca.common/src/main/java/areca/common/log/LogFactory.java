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

import static areca.common.log.ConsoleColors.BLACK_BRIGHT;
import static areca.common.log.ConsoleColors.RED;
import static areca.common.log.ConsoleColors.RED_BOLD;
import static areca.common.log.ConsoleColors.RESET;
import static areca.common.log.ConsoleColors.YELLOW;
import static areca.common.log.ConsoleColors.YELLOW_BOLD;
import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import areca.common.Platform;
import areca.common.base.Supplier.RSupplier;

/**
 *
 * @author Falko Bräutigam
 */
public class LogFactory {

    public enum Level {
        DEBUG, INFO, WARN, ERROR, OFF,
        /** Signals that the logger should use the current {@link LogFactory#DEFAULT_LEVEL}. */
        //DEFAULT
    }

    /** The level that is used for loggers with current level {@link Level#DEFAULT}. */
    public static Level DEFAULT_LEVEL = Level.INFO;

    private static Map<String,Level> levels = new HashMap<>();

    private static volatile int levelsVersion = 0;

    public static Log getLog( Class<?> cl ) {
        return new Log( cl, null );
    }

    public static Log getLog( String prefix, Class<?> cl ) {
        return new Log( cl, prefix );
    }

    public static void setClassLevel( Class<?> cl, Level level ) {
        System.out.println( "LOG: " + cl.getName() + " -> " + level );
        levels.put( cl.getName(), level );
        levelsVersion ++;
    }

    public static void setPackageLevel( Class<?> cl, Level level ) {
        String packageName = StringUtils.substringBeforeLast( cl.getName(), "." );
        System.out.println( "LOG: " + packageName + " -> " + level );
        levels.put( packageName, level );
        levelsVersion ++;
    }

    /**
     * For use with {@link Log#info(String, RSupplier)}.
     */
    public static Object[] a( Object... args ) {
        return args;
    }

    /**
     *
     */
    public static class Log {

        private static final Map<Level,Pair<String,String>> COLORS = new HashMap<>() {{
            put( Level.DEBUG, Pair.of( BLACK_BRIGHT, BLACK_BRIGHT ) );
            put( Level.INFO,  Pair.of( "", "" ) ); // ConsoleColors.RESET );
            put( Level.WARN,  Pair.of( YELLOW_BOLD, YELLOW ) );
            put( Level.ERROR, Pair.of( RED_BOLD, RED ) );
        }};

        protected final String  cl;

        protected final String  prefix;

        protected Level         level;

        protected volatile int  version = -1;


        public Log( Class<?> cl, String prefix ) {
            this.cl = cl.getName();
            this.prefix = prefix != null ? prefix : cl.getSimpleName();
        }

        public boolean isLevelEnabled( Level l ) {
            // find new level if levels map has changed
            if (version != levelsVersion) {
                level = DEFAULT_LEVEL;
                version = levelsVersion;

                var match = "";
                for (var entry : levels.entrySet()) {
                    if (cl.startsWith( entry.getKey() )) {
                        if (entry.getKey().length() > match.length()) {
                            match = entry.getKey();
                            level = entry.getValue();
                        }
                    }
                }
            }
            return l.ordinal() >= level.ordinal();
        }

        public String format( Level msgLevel, String msg, Object... args ) {
            var formatted = args != null ? String.format( msg, args ) : msg;
            var c1 = COLORS.get( msgLevel ).getLeft();
            var c2 = COLORS.get( msgLevel ).getRight();
            return String.format( "[%s%-5s%s] %s%-20s%s:%s %s%s%s",
                    c1, msgLevel, RESET,
                    BLACK_BRIGHT, abbreviate( prefix, 20 ), BLACK_BRIGHT, RESET,
                    c2, formatted, RESET );
        }

        private void doLog( Level msgLevel, String msg, Object[] args, Throwable e ) {
            var out = msgLevel.ordinal() >= Level.ERROR.ordinal() ? System.err : System.out;
            // XXX async
            out.println( format( msgLevel, msg, args ) );
            if (e != null && (Platform.impl == null || Platform.isJVM())) {
                e.printStackTrace( out );
            }
        }

        protected void log( Level msgLevel, String msg, Object[] args, Throwable e ) {
            if (isLevelEnabled( msgLevel )) {
                doLog( msgLevel, msg, args, e );
            }
        }

        protected void log2( Level msgLevel, String msg, RSupplier<Object[]> args, Throwable e ) {
            if (isLevelEnabled( msgLevel )) {
                doLog( msgLevel, msg, args.get(), e );
            }
        }

        public void warn( String msg ) {
            log( Level.WARN, msg, null, null );
        }

        public void warn( String msg, Throwable e ) {
            log( Level.WARN, msg, null, e );
        }

        public void warn( String format, Object... args ) {
            log( Level.WARN, format, args, null );
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
    }
}
