/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.ui.html;

import areca.common.Assert;
import areca.common.base.Opt;
import areca.ui.Color;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.Property.ReadOnly;
import areca.ui.Property.ReadWrite;
import areca.ui.Property.ReadWrites;

/**
 *
 * @author Falko Bräutigam
 */
public class HtmlElement
        extends HtmlEventTarget {

    public static HtmlImplFactory   factory;

    public enum Type {
        DIV
    }

    // instance *******************************************

    public Type                     type = null;

    public ReadWrites<HtmlNode>     children;

    public Styles                   styles;

    public Attributes               attributes;

    public ReadWrite<Boolean>       hidden;

    public ReadOnly<Size>           clientSize;

    public ReadOnly<Size>           offsetSize;

    public ReadOnly<Position>       offsetPosition;


    public HtmlElement( Type type ) {
        this.type = type;
        factory.init( this );
    }


    public HtmlElement() {
        factory.init( this );
    }


    /**
     *
     */
    public static abstract class Attributes
            extends ReadWrites<NameValue> {

        protected Attributes( Object component ) {
            super( component, "attributes" );
        }

        public void set( String name, String value ) {
            doAdd( new NameValue( name, value ) );
        }

        @SuppressWarnings("hiding")
        public Opt<String> opt( String name ) {
            return sequence().first( v -> v.name.equals( name ) ).ifPresentMap( v -> v.value );
        }
    }


    /**
     *
     */
    @SuppressWarnings("hiding")
    public static abstract class Styles
            extends ReadWrites<NameValue> {

        protected Styles( Object component ) {
            super( component, "styles" );
        }

        public abstract Opt<String> opt( String name );

        public Opt<Color> color( String name ) {
            return opt( name ).ifPresentMap( value -> Color.ofHex( value ) );
        }

        public void set( String name, String value ) {
            System.out.print( "Style: " + name + " = " + value );
            doAdd( new NameValue( name, value ) );
        }

        public void set( String name, Color value ) {
            set( name, value.toHex() );
        }

        public void set( String name, Size value ) {
            Assert.isEqual( "", name );
            set( "width", "%spx", value.width() );
            set( "height", "%spx", value.height() );
        }

        public void set( String name, Position value ) {
            Assert.isEqual( "", name );
            set( "left", "%spx", value.x() );
            set( "top", "%spx", value.y() );
        }

        public void set( String name, String format, Object... args ) {
            var value = String.format( format, args );
            set( name, value );
        }

        public void remove( String name ) {
            doRemove( new NameValue( name, null ) );
        }
    }

    /**
     *
     */
    public static class NameValue {

        public String   name;

        public String   value;

        public NameValue( String name, String value ) {
            this.name = name;
            this.value = value;
        }
    }

}
