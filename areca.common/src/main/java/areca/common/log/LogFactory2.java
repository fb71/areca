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

import areca.common.Assert;
import areca.common.base.Sequence;

/**
 *
 * @deprecated Attempt to allow compiler to optimize methods out.
 * @author Falko Bräutigam
 */
public class LogFactory2 {

    public enum Level {
        DEBUG, INFO, WARN, ERROR, OFF
    }

    /** */
    public static Level DEFAULT_LEVEL = Level.WARN;

    private static Map<String,Level> levels = new HashMap<>();

    public static Log getLog( Class<?> cl ) {
        return getLog( null, cl );
    }

    public static Log getLog( String prefix, Class<?> cl ) {
        var logPrefix = prefix != null ? prefix : cl.getSimpleName();
        switch (levelOf( cl )) {
            case OFF: return new Log( logPrefix );
            case ERROR: return new Log( logPrefix );
            case WARN: return new WarnLog( logPrefix );
            case INFO: return new InfoLog( logPrefix );
            case DEBUG: return new DebugLog( logPrefix );
            default: throw new RuntimeException( "Log: unhandled level: ..." );
        }
    }

    protected static Level levelOf( Class<?> cl ) {
        return Sequence.of( levels.entrySet() )
                .filter( entry -> cl.getName().startsWith( entry.getKey() ) )
                .reduce( (e1,e2) -> e1.getKey().length() > e2.getKey().length() ? e1 : e2 )
                .map( entry -> entry.getValue() )
                .orElse( DEFAULT_LEVEL );
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

    /**
     *
     */
    public static class Log {

        protected String  prefix;

        protected Level   level;


        public Log( String prefix ) {
            this.prefix = prefix;
            this.level = Level.OFF;
//            try {
//                System.out.println( "LOG: " ); // + prefix + " -> " + getClass().getSimpleName() );
//            }
//            catch (Throwable e) {
//            }
        }

        protected void log( Level msgLevel, String msg, Object[] args, Throwable e ) {
            Assert.that( msgLevel.ordinal() >= level.ordinal() );
            var formatted = args != null ? String.format( msg, args ) : msg;
            var record = String.format( "[%-5s] %-20s: %s", msgLevel, abbreviate(prefix,20), formatted );
            if (msgLevel.ordinal() >= Level.WARN.ordinal()) {
                System.err.println( record );
            }
            else {
                System.out.println( record );
            }
        }

        public void warn( String msg ) {}

        public void warn( String msg, Throwable e ) {}

        public void info( String format, Object... args ) {}

        public void debug( String format, Object... args ) {}

        public boolean isLevelEnabled( Level l ) {
            return l.ordinal() >= level.ordinal();
        }
    }

    /**
     *
     */
    protected static class WarnLog extends Log {

        public WarnLog( String prefix ) {
            super( prefix );
            this.level = Level.WARN;
        }

        public void warn( String msg ) {
            warn( msg, null );
        }

        public void warn( String msg, Throwable e ) {
            log( Level.WARN, msg, null, e );
        }
    }

    /**
     *
     */
    protected static class InfoLog extends WarnLog {

        public InfoLog( String prefix ) {
            super( prefix );
            this.level = Level.INFO;
        }

        @Override
        public void info( String format, Object... args ) {
            log( Level.INFO, format, args, null );
        }
    }

    /**
     *
     */
    protected static class DebugLog extends InfoLog {

        public DebugLog( String prefix ) {
            super( prefix );
            this.level = Level.DEBUG;
        }

        @Override
        public void debug( String format, Object... args ) {
            log( Level.DEBUG, format, args, null );
        }
    }

}
