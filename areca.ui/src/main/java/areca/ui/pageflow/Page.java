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

import areca.common.Assert;
import areca.ui.component.UIComponent;
import areca.ui.component.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class Page {

    protected PageSite      site;

    UIComponent init( UIComposite parent, @SuppressWarnings("hiding") PageSite site ) {
        this.site = site;
        return Assert.notSame( parent, doInit( parent ) );
    }

    void dispose() {
        doDispose();
        this.site = null;
    }

    protected abstract UIComponent doInit( UIComposite parent );

    protected abstract void doDispose();

    /**
     *
     */
    public static abstract class PageSite {

        // public ReadOnly<PageSite,UIComposite> parent;
    }

}
