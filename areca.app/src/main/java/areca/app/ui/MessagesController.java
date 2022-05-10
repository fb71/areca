/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.app.ui;

import java.util.logging.Logger;

import areca.app.model.Message;
import areca.common.reflect.RuntimeInfo;
import areca.ui.component2.Button;
import areca.ui.component2.UIComposite;
import areca.ui.controller.Context;
import areca.ui.controller.Controller;
import areca.ui.controller.State;
import areca.ui.controller.UI;
import areca.ui.layout.FillLayout;
import areca.ui.viewer.ListModelAdapter;
import areca.ui.viewer.ListViewer;
import areca.ui.viewer.ModelValueTransformer;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class MessagesController
        extends Controller {

    private static final Logger LOG = Logger.getLogger( MessagesController.class.getName() );

    protected FillLayout                lm = new FillLayout();

    @UI(type = Message.class, viewer = ListViewer.class, transformer = Message2StringTransformer.class)
    protected ListModelAdapter<Message> messages;

    @Context(type = Message.class)
    protected State<Message>            selected;

    @UI(type = Boolean.class, label = "ADD")
    protected boolean                   addBtn;

    protected State<String>             title;


    public static class Message2StringTransformer
            implements ModelValueTransformer<Message, String> {

        @Override
        public String transform2UI( Message value ) {
            // XXX Auto-generated method stub
            throw new RuntimeException( "not yet implemented." );
        }
    }


    @Override
    protected void init( @SuppressWarnings("hiding") Site site ) {
        super.init( site );
        subscribeUIEvent( ev -> LOG.info( "Event: " + ev ) )
                .performIf( ev -> isSource( ev, messages ) );
    }


    @Override
    protected void customize( UIComposite container ) {
        container.add( new Button(), b -> {

        });

        //site.newField().adapter( new PropertyAdapter( title ) ).label( "Titel" ).create( container );
    }
}
