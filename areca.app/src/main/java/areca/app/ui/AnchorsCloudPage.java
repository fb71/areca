/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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

import org.apache.commons.lang3.StringUtils;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.RasterLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
public class AnchorsCloudPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( AnchorsCloudPage.class );

    private PageContainer       ui;


    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Contacts" );
        ui.body.layout.set( new RasterLayout() {{
            spacing.set( 5 );
            margins.set( Size.of( 5, 5 ) );
            itemSize.set( Size.of( 74, 68 ) );
            componentOrder.set( (b1, b2) -> StringUtils.compare( ((Button)b1).label.value(), ((Button)b2).label.value() ) );
        }});

        ui.body.layout();
        return ui;
    }

}
