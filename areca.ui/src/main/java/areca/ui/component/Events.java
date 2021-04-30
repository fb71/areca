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
package areca.ui.component;

import java.util.EventObject;

import areca.common.base.Consumer.RConsumer;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Property;
import areca.ui.html.HtmlEventTarget.EventType;
import areca.ui.html.HtmlEventTarget.HtmlEvent;
import areca.ui.html.HtmlEventTarget.HtmlMouseEvent;

/**
 *
 * @author Falko Bräutigam
 */
public class Events
        extends Property<UIComponent,String> {

    private static final Log log = LogFactory.getLog( Events.class );

    protected Events( UIComponent component ) {
        super( component, "events" );
    }

    protected void dispose() {
         component.htmlElm.listeners.clear();
    }

    public void onSelection( RConsumer<SelectionEvent> handler ) {
        component.htmlElm.listeners.add( EventType.CLICK, htmlEv -> {
            SelectionEvent ev = new SelectionEvent( component,  htmlEv );
            handler.accept( ev );
            publish( ev );
        });
    }

//    public void onAction( RConsumer<ActionEvent> handler ) {
//        component.htmlElm.listeners.add( EventType.CLICK, htmlEv -> {
//            SelectionEvent ev = new SelectionEvent( component,  htmlEv );
//            handler.accept( ev );
//            publish( ev );
//        });
//    }

    protected void publish( UIEvent<?> ev ) {
        EventManager.instance().publish( ev );
    }


    /**
     *
     */
    public static abstract class UIEvent<H extends HtmlEvent>
            extends EventObject {

        protected H     htmlEv;

        public UIEvent( UIComponent source, H htmlEv ) {
            super( source );
            this.htmlEv = htmlEv;
        }

        public H underlying() {
            return htmlEv;
        }
    }

    /**
     *
     */
    public static class SelectionEvent
            extends UIEvent<HtmlMouseEvent> {

        public SelectionEvent( UIComponent src, HtmlMouseEvent htmlEv ) {
            super( src, htmlEv );
        }
    }

   /**
    *
    */
   public static class ActionEvent
           extends UIEvent<HtmlMouseEvent> {

       public ActionEvent( UIComponent src, HtmlMouseEvent htmlEv ) {
           super( src, htmlEv );
       }
   }
}
