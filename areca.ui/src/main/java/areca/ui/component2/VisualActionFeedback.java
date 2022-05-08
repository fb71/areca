/*
 * Copyright (C) 2021-2022, the @authors. All rights reserved.
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
package areca.ui.component2;

import java.util.Arrays;

import areca.common.Platform;
import areca.common.event.EventListener;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.Events.UIEvent;

/**
 *
 * @author Falko Bräutigam
 */
public class VisualActionFeedback
        implements EventListener<UIEvent> {

    private static final Log LOG = LogFactory.getLog( VisualActionFeedback.class );

    private static VisualActionFeedback instance;

    public static VisualActionFeedback start() {
        return instance = instance != null ? instance : new VisualActionFeedback();
    }

    // instance *******************************************

    protected VisualActionFeedback() {
        Events.manager
                .subscribe( this )
                .performIf( ev -> ev instanceof UIEvent &&
                        (((UIEvent)ev).type == EventType.ACTION || ((UIEvent)ev).type == EventType.SELECT));
    }

    @Override
    public void handle( UIEvent ev ) {
        LOG.info( "ev: " + ev.getSource() );

        var root = ev.getSource().parent();
        while (root.parent() != null) {
            root = root.parent();
        }
        LOG.info( "root: " + root );

        root.add( new UIComposite() {{
            cssClasses.set( Arrays.asList( "VisualClickFeedbackStart" ) );
            position.set( Position.of( 100, 100 ) );
            Platform.async( () -> {
                cssClasses.set( Arrays.asList( "VisualClickFeedbackEnd" ) );
                Platform.schedule( 1000, () -> {
                    dispose();
                });
            });
        }});


//        var div = App.instance().rootWindow().htmlElm.children.add( new HtmlElement( Type.DIV ) );
//        div.attributes.set( "class", "VisualClickFeedbackStart" );
//        div.styles.set( "", ev.underlying().clientPosition.get().substract( Position.of( 10, 10 ) ) );
//        Platform.async( () -> {
//            div.attributes.set( "class", "VisualClickFeedbackEnd" );
//            Platform.schedule( 1000, () -> {
//                div.remove();
//            });
//        });
    }

}
