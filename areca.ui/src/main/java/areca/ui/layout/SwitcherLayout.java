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

import java.util.ArrayList;
import java.util.List;

import areca.common.MutableInt;
import areca.common.Platform;
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

    private UIComposite     composite;

    private List<Child>     children = new ArrayList<>();

    private Button          leftHandle;

    private Button          rightHandle;

    private int             rotationStep;


    @Override
    public void layout( @SuppressWarnings("hiding") UIComposite composite ) {
        // init
        if (this.composite == null) {
            composite.styles.add( new CssStyle( "perspective", "6000px") );

            // children
            rotationStep = 360 / composite.components.size();
            MutableInt i = new MutableInt( 0 );
            for (var c : composite.components) {
                c.cssClasses.add( "SwitcherChild" );
                c.position.set( Position.of( 0, 0 ) );
                children.add( new Child() {{
                    component = c;
                    rotate( rotationStep * i.getAndIncrement() );
                }});
            }
            // handles
            this.leftHandle = composite.add( new SwitcherHandle( "keyboard_double_arrow_right") {{
                events.on( SELECT, ev -> children.forEach( child -> child.rotate( rotationStep ) ) );
            }});
            this.rightHandle = composite.add( new SwitcherHandle( "keyboard_double_arrow_left")  {{
                events.on( SELECT, ev -> children.forEach( child -> child.rotate( -rotationStep ) ) );
            }});
        }
        this.composite = composite;

        // components
        Size clientSize = composite.clientSize.value();
        children.forEach( child -> child.component.size.set( clientSize ) );

        // handles
        leftHandle.position.set( Position.of(
                -10, (clientSize.height() - leftHandle.size.$().height()) / 2 ) );
        rightHandle.position.set( Position.of(
                clientSize.width() - rightHandle.size.$().width() + 10,
                (clientSize.height() - rightHandle.size.$().height()) / 2 ) );
    }

    /**
     *
     */
    public void next() {
        children.forEach( child -> child.rotate( rotationStep ) );
    }

    /**
     *
     */
    class Child {
        UIComponent component;
        int         rotation;

        public void rotate( int delta ) {
            rotation += delta;
            var style = new CssStyle( "transform", "rotate3d(0,1,0," + rotation + "deg)" );
            component.styles.remove( style ); // XXX check styles Property impl
            component.styles.add( style );
            component.bordered.set( true );
            component.styles.add( new CssStyle( "margin", "-1px" ));
            Platform.schedule( 1000, () -> {
                component.bordered.set( false );
                component.styles.remove( new CssStyle( "margin", "-1px" ));
            });
        }
    }

    /**
     *
     */
    static class SwitcherHandle
            extends Button {

        public static final Size DEFAULT_SIZE = Size.of( 30, 65 );

        public SwitcherHandle() {
            size.set( DEFAULT_SIZE );
            icon.set( "drag_indicator" );
        }

        public SwitcherHandle( String icon ) {
            this();
            this.icon.set( icon );
        }
    }

}
