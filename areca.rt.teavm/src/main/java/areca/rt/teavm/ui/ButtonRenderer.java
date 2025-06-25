/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.rt.teavm.ui;

import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLImageElement;

import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Button;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class ButtonRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( ButtonRenderer.class );

    public static final ClassInfo<ButtonRenderer> TYPE = ButtonRendererClassInfo.instance();

    public static void _start() {
        UIComponentEvent.manager()
                .subscribe( new ButtonRenderer() )
                .performIf( ev -> ev instanceof ComponentConstructedEvent && ev.getSource() instanceof Button );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        Button c = (Button)ev.getSource();

//        var created = (HTMLElement)null;
//        if (c.type.get() == Button.Type.SUBMIT) {
//            // idea was to help the Browser to save password;
//            // but would need a surounding <form>, and never works on chrome
//            created = (HTMLElement)doc().createElement( "input" );
//            created.setAttribute( "type", "submit" );
//            created.setAttribute( "value", c.label.get() );
//        }
        var created = (HTMLButtonElement)doc().createElement( "button" );
        created.setAttribute( "type", "button" );

        c.htmlElm = created;
        var htmlBtn = created;

        // imageData
        c.image.onInitAndChange( (newValue, __) -> {
            if (newValue != null) {
                var img = c.data( "__imageData__", () -> {
                    return (HTMLImageElement)htmlBtn.appendChild( doc().createElement( "img" ) );
                });
                img.setSrc( "data:;base64," + newValue );
                //img.getStyle().setProperty( "height", "100%" );
            }
            else {
                c.<HTMLImageElement>optData( "__imageData__" ).ifPresent( img -> img.delete() );
            }
        });

        // label
        c.label.onInitAndChange( (newValue, __) -> {
            var span = c.data( "__label__", () -> {
                return (HTMLElement)htmlBtn.appendChild( doc().createElement( "span" ) );
            });
            span.setAttribute( "class", "label" );
            switch (c.format.$()) {
                case PLAIN: span.setInnerText( newValue ); break;
                case HTML: span.setInnerHTML( newValue ); break;
            }
        });

        // icon
        c.icon.onInitAndChange( (newValue, __) -> {
            var span = c.data( "__icon__", () -> {
                return (HTMLElement)htmlBtn.appendChild( doc().createElement( "span" ) );
            });
            switch (c.iconStyle.$()) {
                case FILLED : span.setAttribute( "class", "icon material-icon-filled" ); break;
                case OUTLINED : span.setAttribute( "class", "icon material-icon-outlined" ); break;
                case ROUND : span.setAttribute( "class", "icon material-icon-round" ); break;
                case TWOTONE : span.setAttribute( "class", "icon material-icon-twotone" ); break;
            }
            span.setInnerText( newValue );
        });

//        // label
//        var textNode = (HTMLElement)doc().createTextNode( c.label.opt().orElse( "" ) );
//        htmlButton.appendChild( textNode );
//        c.label.onChange( (newValue, oldValue) -> {
//            textNode.setNodeValue( newValue );
//        });
    }
}
