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

import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class TextFieldRenderer
        extends UIComponentRenderer {

    private static final Log LOG = LogFactory.getLog( TextFieldRenderer.class );

    public static final ClassInfo<TextFieldRenderer> TYPE = TextFieldRendererClassInfo.instance();

    static void _start() {
        UIComponentEvent.manager
                .subscribe( new TextFieldRenderer() )
                .performIf( ev -> ev instanceof UIComponentEvent && ev.getSource() instanceof TextField );
    }

    // instance *******************************************

    @Override
    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        TextField c = (TextField)ev.getSource();

        var labelElm = (HTMLElement)doc().createElement( "label" );
        var textNode = (HTMLElement)doc().createTextNode( c.label.opt().orElse( "" ) );
        labelElm.appendChild( textNode );

        var inputElm = (HTMLInputElement)doc().createElement( "input" );
        inputElm.setAttribute( "type", "text" );
        labelElm.appendChild( inputElm );

        c.htmlElm = inputElm;

        c.content.onInitAndChange( (newValue, oldValue) -> {
            inputElm.setAttribute( "value", newValue );
        });

        c.label.onInitAndChange( (newValue, oldValue) -> {
            textNode.setNodeValue( newValue );
        });

        inputElm.addEventListener( "input", htmlEv -> {
            LOG.info( "HTML event: %s", inputElm.getValue() );
            //((MouseEvent)_htmlEv).stopPropagation();
            //htmlEv.preventDefault();
            c.content.rawSet( inputElm.getValue() );
        });

        super.componentConstructed( ev );
    }

}
