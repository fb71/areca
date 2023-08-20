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
import areca.ui.component2.UIComposite;
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

    @Page.Part
    protected PageContainer ui;

    @Page.CreateUI
    protected UIComposite createUI( UIComposite parent ) {
        ui.init( parent ).title.set( "Simple Page" );
        return ui;
    }
}
