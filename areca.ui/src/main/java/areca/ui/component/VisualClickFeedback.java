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

import areca.common.Platform;
import areca.common.event.EventListener;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.App;
import areca.ui.Position;
import areca.ui.component.Events.SelectionEvent;
import areca.ui.html.HtmlElement;
import areca.ui.html.HtmlElement.Type;

/**
 *
 * @author Falko Bräutigam
 */
public class VisualClickFeedback
        implements EventListener<SelectionEvent> {

    private static final Log LOG = LogFactory.getLog( VisualClickFeedback.class );

    private static VisualClickFeedback instance;

    public static VisualClickFeedback start() {
        return instance = instance != null ? instance : new VisualClickFeedback();
    }

    // instance *******************************************

    protected VisualClickFeedback() {
        EventManager.instance()
                .subscribe( this )
                .performIf( ev -> ev instanceof SelectionEvent );
    }

    @Override
    public void handle( SelectionEvent ev ) {
        LOG.info( "ev: " + ev );
        var div = App.instance().rootWindow().htmlElm.children.add( new HtmlElement( Type.DIV ) );
        div.attributes.set( "class", "VisualClickFeedbackStart" );
        div.styles.set( "", ev.underlying().clientPosition.get().substract( Position.of( 10, 10 ) ) );
        Platform.instance().async( () -> {
            div.attributes.set( "class", "VisualClickFeedbackEnd" );
            Platform.instance().schedule( 1000, () -> {
                div.remove();
            });
        });

    }

}
