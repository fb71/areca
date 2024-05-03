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

import org.teavm.jso.dom.html.HTMLOptionElement;
import org.teavm.jso.dom.html.HTMLSelectElement;

import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Select;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class SelectRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( SelectRenderer.class );

    public static final ClassInfo<SelectRenderer> TYPE = SelectRendererClassInfo.instance();

    public static void _start() {
        UIComponentEvent.manager()
                .subscribe( new SelectRenderer() )
                .performIf( ComponentConstructedEvent.class, ev -> ev.getSource() instanceof Select );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        var c = (Select)ev.getSource();

        var select = (HTMLSelectElement)(c.htmlElm = doc().createElement( "select" ));

        c.multiple.onInitAndChange( (newValue, oldValue) -> {
            if (newValue) {
                select.setAttribute( "multiple", "true" );
            } else {
                select.removeAttribute( "multiple" );
            }
        });

        c.options.onInitAndChange( (newValue, oldValue) -> {
            LOG.debug( "options: %s", newValue );
            select.setTextContent( "" ); // XXX replaceChildren()
            for (var value : newValue) {
                var option = (HTMLOptionElement)(doc().createElement( "option" ));
                option.setText( value );
                option.setValue( value );
                select.appendChild( option );
            }
        });

        c.value.onInitAndChange( (newValue, oldValue) -> {
            LOG.debug( "Set: %s", newValue );
            var children = select.getChildNodes();
            for (int i=0; i < children.getLength(); i++) {
                var option = (HTMLOptionElement)children.get( i );
                option.setSelected( option.getValue().equals( newValue ) );
                LOG.debug( "    option: %s", option.getValue() );
            }
        });

        select.addEventListener( "change", htmlEv -> {
            htmlEv.stopPropagation();
            htmlEv.preventDefault();

            c.value.rawSet( select.getValue() );
            LOG.info( "Select: %s", select.getValue() );
            propagateEvent( c, htmlEv, EventType.TEXT );
        });
    }

}
