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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.NoRuntimeInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Action;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.AbsoluteLayout;
import areca.ui.pageflow.Page.PageSite;

/**
 * Provides the standard common user interface elements for a {@link Page}.
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class PageContainer
        extends UIComposite {

    static final Log LOG = LogFactory.getLog( PageContainer.class );

    public static final ClassInfo<PageContainer> INFO = PageContainerClassInfo.instance();

    private static final String CSS_HEADER = "PageHeader";
    private static final String CSS_HEADER_ITEM = "PageHeaderItem";
    private static final String CSS_TITLE = "PageTitle";

    public ReadWrite<?,String>  title = Property.rw( this, "title" );

    public UIComposite          body;

    protected UIComposite       headerComposite;

    protected Text              titleText;

    protected Button            closeBtn;

    protected Map<Action,Button> actionsBtns = new HashMap<>();

    @Page.Context
    protected PageSite          pageSite;

    /**
     * No-op ctor for injection.
     */
    public PageContainer() {}

    /**
     * Init after injection.
     */
    public PageContainer init( UIComposite parent ) {
        Assert.notNull( pageSite, "PageSite is not injected. Using @Page.Part?" );
        doInit( parent );
        return this;
    }

    /**
     * Ctor for {@link Page}s not using injection.
     */
    public PageContainer( Page page, UIComposite parent ) {
        this.pageSite = page.pageSite;
        doInit( parent );
    }

    protected void doInit( UIComposite parent ) {
        parent.components.add( this ); // XXX
        layout.set( new PageContainerLayout() );

        // header
        headerComposite = add( new UIComposite() {{
            cssClasses.add( CSS_HEADER );

            // closeBtn
            if (Pageflow.current().pages().count() > 1) {
                closeBtn = add( new Button() {{
                    cssClasses.add( CSS_HEADER_ITEM );
                    icon.set( "arrow_back" );
                    events.on( SELECT, ev -> {
                        pageSite.close();
                    });
                }});
            }

            // title
            titleText = add( new Text() {{
                cssClasses.add( CSS_TITLE );
                title.onChange( (newValue, __) -> content.set( newValue ) );
            }});

            // actions
            pageSite.actions.onChange( (actions, __) -> {
                for (var action : actions) {
                    actionsBtns.computeIfAbsent( action, ___ -> add( new Button() {{
                        //bordered.set( false );
                        cssClasses.add( CSS_HEADER_ITEM );
                        action.order.opt().ifAbsent( () -> action.order.set( actionsBtns.size() ) );
                        action.label.opt().ifPresent( v -> label.set( v ) );
                        action.icon.onInitAndChange( (v,____) -> icon.set( v ) );
                        action.type.onInitAndChange( (v,____) -> type.set( v ) );
                        action.enabled.onInitAndChange( (v,____) -> enabled.set( v ) );
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
                //PageContainer.this.layout_();
            });
        }});

        body = add( new UIComposite() );
    }

    /**
     *
     */
    @NoRuntimeInfo
    public <C extends UIComponent> C addAction( int index, C btn ) {
        btn.cssClasses.add( CSS_HEADER_ITEM );
        headerComposite.add( btn );
        return btn;
    }

    /**
     *
     */
    class PageContainerLayout
            extends AbsoluteLayout {

        public static final int HEADER_HEIGHT = 60;
        public static final int TOOLBAR_HEIGHT = 45;

        @Override
        public void layout( UIComposite composite ) {
            super.layout( composite );
            if (PageContainer.this.clientSize.opt().isAbsent()) {
                return;
            }
            LOG.debug( "clientSize: %s", PageContainer.this.clientSize.get() );
            @SuppressWarnings( "hiding" )
            var clientSize = PageContainer.this.clientSize.get();

            var top = 0;
            headerComposite.position.set( Position.of( 0, top ) );
            headerComposite.size.set( Size.of( clientSize.width(), HEADER_HEIGHT ) );
            top += HEADER_HEIGHT;

            var btnSize = HEADER_HEIGHT - 10;
            var btnMargin = (HEADER_HEIGHT - btnSize) / 2;
            if (closeBtn != null) {
                closeBtn.position.set( Position.of( btnMargin, btnMargin ) );
                closeBtn.size.set( Size.of( btnSize, btnSize ) );
            }

            var titleMargin = (HEADER_HEIGHT - 22) / 2;
            titleText.position.set( Position.of( btnMargin + btnSize + titleMargin, titleMargin-1 ) );
            // XXX titleText.size.set( Size.of( ) );

            // less rendering (flickering, font loading) during Page opening
            Platform.schedule( 100, () -> {
                if (composite.isDisposed()) {
                    return;
                }
                var actionLeft = clientSize.width() - btnSize - btnMargin;
                var sorted = new ArrayList<>( actionsBtns.keySet() );
                sorted.sort( (l,r) -> -l.order.$().compareTo( r.order.$() ) );
                for (var action : sorted) {
                    var btn = actionsBtns.get( action );
                    btn.position.set( Position.of( actionLeft, btnMargin ) );
                    btn.size.set( Size.of( btnSize, btnSize ) );
                    actionLeft -= btnSize; // + btnMargin;
                }
            });

            body.position.set( Position.of( 0, top ) );
            body.size.set( Size.of( clientSize.width(), clientSize.height() - top ) );
        }
    }

}
