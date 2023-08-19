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
package areca.demo;

import org.teavm.jso.dom.html.HTMLElement;
import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Size;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.layout.SwitcherLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.Page.PageSite;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class StartPage {

    private static final Log LOG = LogFactory.getLog( StartPage.class );

    public static final ClassInfo<StartPage> INFO = StartPageClassInfo.instance();

    @Page.Context
    protected CMS           cms;

    @Page.Context
    protected PageSite      pageSite;

    @Page.Part
    protected PageContainer ui;


    @Page.Init
    public void init() {
        LOG.info( "init: pageSite = %s", pageSite );
    }


    @Page.CreateUI
    public UIComponent create( UIComposite parent ) {
        LOG.info( "createUI: CMS = %s, ui = %s", cms, ui );
        ui.init( parent ).title.set( "Areca Demo" );

//        ui.body.layout.set( RowLayout.filled() );
        ui.body.layout.set( SwitcherLayout.defaults() );

        var one = ui.body.add( new ScrollableComposite() {{
            layout.set( RowLayout.filled().margins.set( Size.of( 20, 10 ) ) );
            add( createText( cms.file( "start" ) ) );
        }});
        var two = ui.body.add( new ScrollableComposite() {{
            layout.set( RowLayout.filled().margins.set( Size.of( 20, 10 ) ) );
            add( createText( cms.file( "programming" ) ) );
        }});

        Platform.schedule( 1000, () -> { // XXX listen to ComponentConstructedEvent
            new SyncScrolling( one, two );
        });
        return ui;
    }


    protected Text createText( CMS.File file ) {
        return new Text() {{
            format.set( Format.HTML );
            content.set( "..." );
            file.content().onSuccess( fileContent -> {
                String md = fileContent.orElse( "404!  (" + file.url() + ")" );
                content.set( Marked.instance().parse( md ) );

                tweakHtml( this );
            });
        }};
    }


    protected void tweakHtml( Text text ) {
        Platform.schedule( 200, () -> { // XXX listen to ComponentConstructedEvent
            if (text.htmlElm == null) {
                LOG.info( "tweak links: no element rendered yet" );
                tweakHtml( text );
            }
            else {
                tweakHtmlLinks( text );
                Prism.instance().highlightAllUnder( (HTMLElement)text.htmlElm );
            }
        });
    }


    protected void tweakHtmlHeaders( Text text ) {
        var links = ((HTMLElement)text.htmlElm).getElementsByTagName( "a" );
        for (int i = 0; i < links.getLength(); i++) {
            HTMLElement a = links.item( i );
        }
    }


    protected void tweakHtmlLinks( Text text ) {
        var links = ((HTMLElement)text.htmlElm).getElementsByTagName( "a" );
        for (int i = 0; i < links.getLength(); i++) {
            HTMLElement a = links.item( i );
            var href = a.getAttribute( "href" );
            LOG.info( "a: %s", href );

//            // internal/article link
//            if (href.startsWith( "#" ) || !href.contains( ":" )) {
//                a.addEventListener( "click", htmlEv -> {
//                    WebsiteApp.catchAll( () -> {
//                        LOG.info( "CLICK!" );
//                        htmlEv.preventDefault();
//
//                        var file2Load = cms.file( href.replaceFirst( "[#!]", "" ) );
//                        var newPage = new ArticlePage();
//                        newPage.file = file2Load;
//                        pageSite.createPage( newPage )
//                        .putContext( file2Load, Context.DEFAULT_SCOPE )
//                        .parent( ArticlePage.this )
//                        .open();
//                        return null;
//                    });
//                });
//            }
//            // external
//            else if (href.startsWith( "http" )) {
//                a.setAttribute( "target", "_blank" );
//                a.setAttribute( "rel", "noopener" );
//            }
        }
    }

}
