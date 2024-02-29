/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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
package areca.rt.teavm.ui.mdb;

import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLTextAreaElement;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.rt.teavm.ui.TextFieldRenderer;
import areca.ui.component2.TextField;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class MDBTextFieldRenderer
        extends TextFieldRenderer {

    private static final Log LOG = LogFactory.getLog( MDBTextFieldRenderer.class );

    public static final ClassInfo<MDBTextFieldRenderer> INFO = MDBTextFieldRendererClassInfo.instance();

    public static void _start() {
        UIComponentEvent.manager()
                .subscribe( new MDBTextFieldRenderer() )
                .performIf( ComponentConstructedEvent.class, ev -> ev.getSource() instanceof TextField );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        TextField c = (TextField)ev.getSource();

        // multiline textarea
        if (c.multiline.get()) {
            var textarea = (HTMLTextAreaElement)(c.htmlElm = doc().createElement( "textarea" ));

            c.content.onInitAndChange( (newValue, oldValue) -> {
                LOG.debug( "Set: %s", newValue );
                textarea.setValue( newValue != null ? newValue : "" );
            });
            textarea.addEventListener( "input", htmlEv -> {
                htmlEv.stopPropagation();
                htmlEv.preventDefault();

                c.content.rawSet( textarea.getValue() );
                LOG.debug( "Text: %s", textarea.getValue() );
                propagateEvent( c, htmlEv );
            });
        }
        // input
        else {
            var outline = (HTMLElement)(c.htmlElm = doc().createElement( "div" ));
            c.cssClasses.setThemeClasses( "form-outline" );
            outline.setAttribute( "class", "form-outline" );
            //outline.setAttribute( "data-mdb-input-init", "" );

            var id = String.valueOf( c.hashCode() );

            var input = (HTMLInputElement)(doc().createElement( "input" ));
            input.setAttribute( "type", "text" );
            input.setAttribute( "class", "form-control" );
            input.setAttribute( "id", id );
            outline.appendChild( input );

            var label = (HTMLElement)(doc().createElement( "label" ));
            label.setAttribute( "class", "form-label" );
            label.setAttribute( "for", id );
            outline.appendChild( label );

            label.appendChild( doc().createTextNode( "Label" ) );

            c.content.onInitAndChange( (newValue, oldValue) -> {
                LOG.debug( "Set: %s", newValue );
                input.setValue( newValue != null ? newValue : "" );
            });

            LOG.debug( "Register listener: %s", "input" );
            input.addEventListener( "input", htmlEv -> {
                htmlEv.stopPropagation();
                htmlEv.preventDefault();

                c.content.rawSet( input.getValue() );
                LOG.debug( "Text: %s", input.getValue() );
                propagateEvent( c, htmlEv );
            });
        }
    }

}
