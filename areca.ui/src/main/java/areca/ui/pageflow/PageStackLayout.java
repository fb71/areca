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

import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_CLOSING;
import static areca.ui.pageflow.PageflowEvent.EventType.PAGE_OPENED;

import areca.common.Platform;
import areca.common.event.EventCollector;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component2.UIComponent.CssStyle;
import areca.ui.component2.UIComposite;
import areca.ui.layout.AbsoluteLayout;
import areca.ui.layout.LayoutManager;

/**
 * A simple {@link PageLayout} that positions every next {@link Page} on top of all
 * others. This layout provides a open/close animation via CSS classes
 * {@link #CSS_PAGE_OPENING} and {@link #CSS_PAGE_CLOSING}.
 *
 * @implNote Listens to {@link PageflowEvent}s
 *
 * @author Falko Bräutigam
 */
public class PageStackLayout
        extends AbsoluteLayout
        implements PageLayout {

    private static final Log LOG = LogFactory.getLog( PageStackLayout.class );

    private static final String CSS_PAGE_CLOSING = "PageClosing";
    private static final String CSS_PAGE_OPENING = "PageOpening";

    protected PageLayoutSite    site;


    public PageStackLayout( PageLayoutSite site ) {
        this.site = site;

        var collector = new EventCollector<PageflowEvent>( 100 );
        EventManager.instance()
                .subscribe( (PageflowEvent ev) -> {
                    // open
                    if (ev.type == PAGE_OPENED) {
                        LOG.debug( "PageflowEvent: %s (%s)", ev.type, ev.clientPage.getClass().getSimpleName() );
                        ev.pageUI.cssClasses.add( CSS_PAGE_OPENING );
                        // give the new page its size so that PageOpening animation works
                        layout( site.container() );
                    }
                    // close
                    else if (ev.type == PAGE_CLOSING) {
                        LOG.debug( "PageflowEvent: %s (%s)", ev.type, ev.clientPage.getClass().getSimpleName() );
                        ev.pageUI.cssClasses.add( CSS_PAGE_CLOSING );
                        ev.pageUI.position.set( ev.pageUI.position.$().add( 0, site.container().clientSize.value().height() / 4 ) );
                    }

                    // deferred layout
                    collector.collect( ev, events -> {
                        LOG.debug( "Layout: (%s) : %s", site.container().components.size(),
                                site.pageflow().pages().reduce( "", (r,p) -> r + p.getClass().getSimpleName() + ", " ) );

                        for (var _ev : events) {
                            // opened
                            if (_ev.type == PAGE_OPENED) {
                                _ev.pageUI.styles.add( CssStyle.of( "transition-delay", Platform.isJVM() ? "0.15s" : "0.2s" ) );
                                _ev.pageUI.cssClasses.remove( CSS_PAGE_OPENING );

                                // createUI() *after* PageRoot composite is rendered with PageOpening CSS
                                // class to make sure that Page animation starts after given delay no matter
                                // what the createUI() method does
                                _ev.page.createUI( _ev.pageUI );

                                Platform.schedule( 1000, () -> {
                                    _ev.pageUI.styles.remove( CssStyle.of( "transition-delay", "0.2s") );
                                });
                            }
                            // closed
                            else if (_ev.type == PAGE_CLOSING) {
                                Platform.schedule( 500, () -> { // time PageClosing animation
                                    if (!_ev.pageUI.isDisposed()) {
                                        _ev.pageUI.dispose();
                                    }
                                });
                            }
                        }
                        layout(); // FIXME layout just the size changed pages
                    });
                })
                .performIf( PageflowEvent.class, ev -> ev.getSource() == site.pageflow() )
                .unsubscribeIf( () -> site.pageflow().isDisposed() );
    }


    protected void layout() {
        layout( site.container() );

        // XXX nur die, die sich geändert haben
        for (var child : site.container().components.value()) {
            if (child instanceof UIComposite ) {
                ((UIComposite)child).layout();
            }
        }
    }


    @Override
    public void layout( UIComposite composite ) {
        super.layout( composite );

        // int zIndex = 0;
        for (var component : composite.components) {
            if (site.page( (UIComposite)component ).isPresent()) { // closing page
                component.position.set( Position.of( 0, 0 ) );
                component.size.set( composite.clientSize.value() );
                // component.zIndex.set( zIndex++ );
            }
        }
    }


    @Override
    public LayoutManager manager() {
        return this;
    }

}
