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
package areca.rt.teavm.html;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Property;
import areca.ui.Size;
import areca.ui.Property.ReadWrites;
import areca.ui.html.HtmlElement;
import areca.ui.html.HtmlElement.Attributes;
import areca.ui.html.HtmlElement.NameValue;
import areca.ui.html.HtmlElement.Styles;
import areca.ui.html.HtmlNode;

/**
 *
 * @author Falko Bräutigam
 */
public class HtmlElementImpl {

    private static final Log LOG = LogFactory.getLog( HtmlElementImpl.class );


    public static void init( HtmlElement elm, HTMLDocument doc ) {
        LOG.debug( "elm= " + elm );
        HTMLElement delegate = (HTMLElement)doc.createElement( "div" );
        init( elm, delegate );
        doc.getBody().appendChild( delegate );  // FIXME
    }


    public static void init( HtmlElement elm, HTMLElement delegate ) {
        elm.delegate = delegate;

        HtmlEventTargetImpl.init( elm, delegate );

        // children
        elm.children = new ReadWrites<>( elm, "children" ) {
            @Override
            protected void doAdd( HtmlNode child ) {
                delegate.appendChild( child.delegate() );
            }
            @Override
            protected void doRemove( HtmlNode value ) {
                throw new RuntimeException( "not yet implemented." );
            }
            @Override
            public Sequence<HtmlNode,RuntimeException> sequence() {
                throw new RuntimeException( "not yet implemented." );
            }
        };

        // attributes
        elm.attributes = new Attributes( elm ) {
            @Override
            protected void doAdd( NameValue value ) {
                LOG.info( "Attributes: %s = %s", value.name, value.value );
                delegate.setAttribute( value.name, value.value );
            }
            @Override
            protected void doRemove( NameValue value ) {
                delegate.removeAttribute( value.name );
            }
            @Override
            @SuppressWarnings("hiding")
            public Opt<String> opt( String name ) {
                return Opt.ofNullable( delegate.getAttribute( name ) );
            }
            @Override
            public Sequence<NameValue,RuntimeException> sequence() {
                var length = delegate.getAttributes().getLength();
                return Sequence
                        .series( 0, n -> n + 1, n -> n < length)
                        .map( n -> delegate.getAttributes().item( n ) )
                        .map( node -> new NameValue( node.getName(), node.getValue() ) );
            }
        };

        // styles
        elm.styles = new Styles( elm ) {
            @Override
            @SuppressWarnings("hiding")
            public Opt<String> opt( String name ) {
                return Opt.ofNullable( delegate.getStyle().getPropertyValue( name ) );
            }
            @Override
            protected void doAdd( NameValue value ) {
                LOG.info( "Styles: %s = %s", value.name, value.value );
                delegate.getStyle().setProperty( value.name, value.value );
            }
            @Override
            protected void doRemove( NameValue value ) {
                delegate.getStyle().removeProperty( value.name );
            }
            @Override
            public Sequence<NameValue,RuntimeException> sequence() {
                throw new RuntimeException( "not yet implemented." );
            }
        };

        // hidden
        elm.hidden = Property.create( elm, "hidden", () -> delegate.isHidden(), v -> delegate.setHidden( v ) );

        // clientSize
        elm.clientSize = Property.create( elm, "clientSize", () ->
                Size.of( delegate.getClientWidth(), delegate.getClientHeight() ) );


        // offsetSize
        elm.offsetSize = Property.create( elm, "offsetSize", () ->
                Size.of( delegate.getOffsetWidth(), delegate.getOffsetHeight() ) );

        // offsetPosition
        elm.offsetPosition = Property.create( elm, "offsetPosition", () ->
                Position.of( delegate.getOffsetLeft(), delegate.getOffsetTop() ) );
    }


    protected static HTMLElement delegate( HtmlNode elm ) {
        return (HTMLElement)elm.delegate;
    }

}
