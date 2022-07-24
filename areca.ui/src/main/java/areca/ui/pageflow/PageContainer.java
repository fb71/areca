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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Action;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComposite;
import areca.ui.layout.LayoutManager;

/**
 * Provides the standard common user interface elements for a {@link Page}.
 *
 * @author Falko Bräutigam
 */
public class PageContainer
        extends UIComposite {

    static final Log LOG = LogFactory.getLog( PageContainer.class );

    private static final String CSS_HEADER = "PageHeader";
    private static final String CSS_HEADER_ITEM = "PageHeaderItem";
    private static final String CSS_TITLE = "PageTitle";

    public ReadWrite<?,String>  title = Property.rw( this, "title" );

    public UIComposite          body;

    protected UIComposite       headerComposite;

    protected Text              titleText;

    //protected UIComposite       toolbar;

    protected Button            closeBtn;

    protected Map<Action,Button> actionsBtns = new HashMap<>();


    public PageContainer( Page page, UIComposite parent ) {
        parent.components.add( this );
        layout.set( new PageContainerLayout() );

        // header
        headerComposite = add( new UIComposite() {{
            cssClasses.add( CSS_HEADER );

            // closeBtn
            closeBtn = add( new Button() {{
                cssClasses.add( CSS_HEADER_ITEM );
                icon.set( "arrow_back" );
                events.on( SELECT, ev -> {
                    Pageflow.current().close( page );
                });
            }});

            // title
            titleText = add( new Text() {{
                cssClasses.add( CSS_TITLE );
                title.onChange( (newValue, __) -> content.set( newValue ) );
            }});

            // actions
            page.pageSite.actions.onChange( (actions, __) -> {
                for (var action : actions) {
                    actionsBtns.computeIfAbsent( action, ___ -> add( new Button() {{
                        bordered.set( false );
                        cssClasses.add( CSS_HEADER_ITEM );
                        action.label.opt().ifPresent( v -> label.set( v ) );
                        action.icon.opt().ifPresent( v -> icon.set( v ) );
                        action.description.opt().ifPresent( v -> tooltip.set( v ) );
                        events.on( SELECT, ev -> {
                            try {
                                action.handler.value().accept( ev );
                            }
                            catch (Exception e) {
                                throw (RuntimeException)e;
                            }
                        });
                    }}));
                }
                // do not layout body and its children
                PageContainer.this.layout.$().layout( PageContainer.this );
            });
        }});

        body = add( new UIComposite() );
    }


    /**
     *
     */
    class PageContainerLayout
            extends LayoutManager {

        public static final int HEADER_HEIGHT = 60;
        public static final int TOOLBAR_HEIGHT = 45;

        @Override
        public void layout( UIComposite composite ) {
            @SuppressWarnings("hiding")
            var clientSize = PageContainer.this.clientSize.opt().orElse( Size.of( 50, 50 ) );

            var top = 0;
            headerComposite.position.set( Position.of( 0, top ) );
            headerComposite.size.set( Size.of( clientSize.width(), HEADER_HEIGHT ) );
            top += HEADER_HEIGHT;

            var btnSize = HEADER_HEIGHT - 10;
            var btnMargin = (HEADER_HEIGHT - btnSize) / 2;
            closeBtn.position.set( Position.of( btnMargin, btnMargin ) );
            closeBtn.size.set( Size.of( btnSize, btnSize ) );

            var titleMargin = (HEADER_HEIGHT - 18) / 2;
            titleText.position.set( Position.of( btnMargin + btnSize + titleMargin, titleMargin-1 ) );

            var actionLeft = clientSize.width() - btnSize - btnMargin;
            for (Button actionBtn : actionsBtns.values()) {
                actionBtn.position.set( Position.of( actionLeft, btnMargin ) );
                actionBtn.size.set( Size.of( btnSize, btnSize ) );
                actionLeft -= btnSize + btnMargin;
            }

            body.position.set( Position.of( 0, top ) );
            body.size.set( Size.of( clientSize.width(), clientSize.height() - top ) );
        }
    }

}
