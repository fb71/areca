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
package areca.ui.pageflow;

import static areca.ui.component2.Events.EventType.SELECT;

import java.util.HashMap;
import java.util.Map;

import areca.common.Assert;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Action;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComposite;
import areca.ui.layout.AbsoluteLayout;
import areca.ui.layout.RowConstraints;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page.PageSite;

/**
 * Provides the standard common user interface elements for a {@link Page}.
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class DialogContainer
        extends UIComposite {

    static final Log LOG = LogFactory.getLog( DialogContainer.class );

    public static final ClassInfo<DialogContainer> INFO = DialogContainerClassInfo.instance();

    public static final int HEADER_HEIGHT = 60;
    public static final int ACTIONS_HEIGHT = 60;

    private static final String CSS_BACK = "DialogBackPane";
    private static final String CSS_HEADER = "DialogHeader";
    private static final String CSS_ACTIONS = "DialogActions";
    private static final String CSS_HEADER_ITEM = "DialogHeaderItem";
    private static final String CSS_TITLE = "DialogTitle";

    public ReadWrite<DialogContainer,String> title = Property.rw( this, "title" );

    public ReadWrite<DialogContainer,Size>   dialogSize = Property.rw( this, "dialogSize" );

    public UIComposite          body;

    public UIComposite          actions;

    protected UIComposite       header;

    protected Text              titleText;

    protected Button            closeBtn;

    protected Map<Action,Button> actionsBtns = new HashMap<>();

    @Page.Context
    protected PageSite          pageSite;

    /**
     * No-op ctor for injection.
     */
    public DialogContainer() {}

    /**
     * Init after injection.
     */
    public DialogContainer init( UIComposite parent ) {
        Assert.notNull( pageSite, "PageSite is not injected. Using @Page.Part?" );
        doInit( parent );
        return this;
    }

    /**
     * Ctor for {@link Page}s not using injection.
     */
    public DialogContainer( Page page, UIComposite parent ) {
        this.pageSite = page.pageSite;
        doInit( parent );
    }

    protected void doInit( UIComposite parent ) {
        parent.components.add( this );

        // center dialog
        layout.set( new AbsoluteLayout() {
            @Override
            public void layout( UIComposite composite ) {
                composite.clientSize.opt().ifPresent( s -> {
                    var dialog = Sequence.of( composite.components ).single();
                    dialog.position.set( Position.of(
                            (s.width() - dialogSize.$().width()) / 2,
                            ((s.height() - dialogSize.$().height()) / 2) - 50 ) );
                    dialog.size.set( Size.of(
                            dialogSize.$().width(),
                            dialogSize.$().height() ) );
                });
            }
        });

        // dialog
        add( new UIComposite() {{
            cssClasses.add( "Dialog" );
            bordered.set( true );
            layout.set( new DialogLayout() );

            // body
            body = add( new UIComposite() );

            // header
            header = add( new UIComposite() {{
                cssClasses.add( CSS_HEADER );
                layout.set( RowLayout.defaults().fillWidth( true ).spacing( 15 ).margins( 5, 5 ) );

                // closeBtn
                closeBtn = add( new Button() {{
                    cssClasses.add( CSS_HEADER_ITEM );
                    lc( RowConstraints.height( HEADER_HEIGHT - 10 ).width.set( HEADER_HEIGHT - 10 ) );
                    type.set( Button.Type.NAVIGATE );
                    bordered.set( false );
                    icon.set( "arrow_back" );
                    events.on( SELECT, ev -> {
                        pageSite.close();
                    });
                }});

                // title
                titleText = add( new Text() {{
                    cssClasses.add( CSS_TITLE );
                    styles.add( CssStyle.of( "line-height", (HEADER_HEIGHT - 10) + "px" ) );
                    lc( RowConstraints.height( HEADER_HEIGHT - 10 ) );
                    title.onChange( (newValue, __) -> content.set( newValue ) );
                }});
            }});

            // actions
            actions = add( new UIComposite(){{
                cssClasses.add( CSS_ACTIONS );
                layout.set( RowLayout.filled().spacing( 15 ).margins( 10, 10 ) );
//                add( new UIComposite() {{
//                    // just eat space left before actions
//                }});
            }});

        }});
    }


    /**
     *
     */
    class DialogLayout
            extends AbsoluteLayout {

        @Override
        public void layout( UIComposite composite ) {
            super.layout( composite );
            composite.clientSize.opt().ifPresent( s -> {
                header.position.set( Position.of( 0, 0 ) );
                header.size.set( Size.of( s.width(), HEADER_HEIGHT ) );

                actions.position.set( Position.of( 0, s.height() - ACTIONS_HEIGHT ) );
                actions.size.set( Size.of( s.width(), ACTIONS_HEIGHT ) );

                body.position.set( Position.of( 0, HEADER_HEIGHT ) );
                body.size.set( Size.of( s.width(), s.height() - HEADER_HEIGHT - ACTIONS_HEIGHT ) );
            });
        }
    }

}
