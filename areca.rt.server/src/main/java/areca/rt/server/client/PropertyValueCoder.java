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
package areca.rt.server.client;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.split;

import java.util.Collection;

import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Events.EventHandler;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Property.ReadWrites;
import areca.ui.component2.Text.Format;

/**
 *
 * @author Falko Bräutigam
 */
public class PropertyValueCoder {

    private static final Log LOG = LogFactory.getLog( PropertyValueCoder.class );

    private static final String NULL = "null";

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public static String encode( Property<?,?> prop, Object value ) {
        if (value == null) {
            return NULL;
        }
        else if (prop instanceof ReadWrites) {
            if (prop.name().equals( "cssClasses" )) {
                var v = Sequence.of( (Collection<String>)value ).reduce( "", (r,n) -> r + ":" + n );
                return String.format( "C%s", v );
            }
            else if (prop.name().equals( "events" )) {
                var v = Sequence.of( (Collection<EventHandler>)value ).reduce( "", (r,n) -> r + ":" + n.type );
                return String.format( "E%s", v );
            }
        }
        else if (prop instanceof ReadWrite) {
            if (value instanceof String) {
                return String.format( "s:%s", value );
            }
//            else if (v instanceof Boolean) {
//                return String.format( "B:%s", v );
//            }
            else if (value instanceof Position) {
                return String.format( "P:%s:%s", ((Position)value).x(), ((Position)value).y() );
            }
            else if (value instanceof Size) {
                return String.format( "S:%s:%s", ((Size)value).width(), ((Size)value).height() );
            }
            else if (value instanceof Enum) {
                var v = (Enum)value;
                return String.format( "e:%s:%s", v.getClass().getName(), v.toString() );
            }
        }
        //LOG.warn( "Missing property: " + prop );
        return "missing";
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public static void decode( Property<?,?> prop, String newValue ) {
        //LOG.info( "Property: %s", prop.name() );
        if (newValue.equals( "missing" )) {
            //LOG.info( "MISSING: %s", prop.name() );
        }
        else if (newValue.equals( NULL )) {
            ((ReadWrite<?,?>)prop).set( null );
        }
        else {
            var magic = newValue.charAt( 0 );
            var value = newValue.substring( 2 );
            switch (magic) {
                case 's': ((ReadWrite<?,String>)prop).set( value ); break;
                case 'B': ((ReadWrite<?,Boolean>)prop).set( Boolean.parseBoolean( value ) ); break;
                case 'C': ((ReadWrite<?,Collection>)prop).set( Sequence.of( split( value, ":" ) ).toList() ); break;
                case 'P': {
                    var parts = split( value, ":" );
                    ((ReadWrite<?,Position>)prop).set( Position.of( parseInt( parts[0] ), parseInt( parts[1] ) ) );
                    break;
                }
                case 'S': {
                    var parts = split( value, ":" );
                    ((ReadWrite<?,Size>)prop).set( Size.of( parseInt( parts[0] ), parseInt( parts[1] ) ) );
                    break;
                }
                case 'e': {
                    var parts = split( value, ":" );
                    Enum v = null;
                    if (parts[0].equals( Format.class.getName() )) {
                        v = Format.valueOf( parts[1] );
                    } else {
                        throw new RuntimeException( "Unknown enum type: " + parts[0] );
                    }
                    ((ReadWrite<?,Format>)prop).set( (Format)v );
                    break;
                }
                default: {
                    LOG.warn( "UNHANDLED: " + prop );
                }
            }
        }
    }
}
