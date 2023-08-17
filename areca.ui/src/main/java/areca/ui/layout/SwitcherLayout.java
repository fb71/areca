/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.ui.layout;

import static areca.ui.component2.Events.EventType.SELECT;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public class SwitcherLayout
        extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( SwitcherLayout.class );

    public static LayoutManager defaults() {
        return new SwitcherLayout();
    }

    // instance *******************************************

    private UIComposite composite;

    private Button leftHandle;

    private Button rightHandle;


    @Override
    public void layout( @SuppressWarnings("hiding") UIComposite composite ) {
        if (this.composite == null) {
            composite.styles.add( new CssStyle( "perspective", "4000px") );
            this.leftHandle = composite.add( new HandleButton() {{
                events.on( SELECT, ev -> {
                    composite.components.values().first().get().styles.add( new CssStyle( "transform", "rotate3d(0,1,0,0deg)" ));
                });
            }});
            this.rightHandle = composite.add( new HandleButton()  {{
                events.on( SELECT, ev -> {
                    UIComponent c = composite.components.values().first().get();
                    c.styles.add( new CssStyle( "transform", "rotate3d(0,1,0,180deg)" ));
                    c.bordered.set( true );
                });
            }});
        }
        this.composite = composite;

        // int zIndex = 0;
        Size clientSize = composite.clientSize.value();
        for (var component : composite.components) {
            if (!(component instanceof HandleButton)) {
                component.cssClasses.add( "SwitcherChild" );
                component.position.set( Position.of( 0, 0 ) );
                component.size.set( clientSize );
                // component.zIndex.set( zIndex++ );
            }
        }

        // handles
        leftHandle.position.set( Position.of(
                0, (clientSize.height() - leftHandle.size.$().height()) / 2 ) );
        rightHandle.position.set( Position.of(
                clientSize.width() - rightHandle.size.$().width(),
                (clientSize.height() - rightHandle.size.$().height()) / 2 ) );
    }

    /**
     *
     */
    static class HandleButton
            extends Button {

        public static final Size DEFAULT_SIZE = Size.of( 40, 40 );

        public HandleButton() {
            size.set( DEFAULT_SIZE );
            icon.set( "drag_indicator" );
            cssClasses.add( "SwitcherHandle" );
        }
    }

}
