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

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Size;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.Page.PageSite;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class SimplePage {

    private static final Log LOG = LogFactory.getLog( SimplePage.class );

    public static final ClassInfo<SimplePage> INFO = SimplePageClassInfo.instance();

    @Page.Context
    protected PageSite      pageSite;

    @Page.Context
    protected CMS           cms;

    @Page.Part
    protected PageContainer ui;


    @Page.CreateUI
    protected UIComposite createUI( UIComposite parent ) {
        ui.init( parent ).title.set( "About" );

        ui.body.layout.set( RowLayout.filled().margins.set( Size.of( 20, 10 ) ) );
        ui.body.add( new ScrollableComposite() {{
            add( new Text() {{
                format.set( Format.HTML );
                content.set( "..." );
                cms.file( "about" ).content().onSuccess( fileContent -> {
                    String md = fileContent.orElse( "404!  (about.md)" );
                    content.set( Marked.instance().parse( md ) );
                });
            }});
        }});

        return ui;
    }
}
