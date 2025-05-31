/*
 * Copyright (C) 2025, the @authors. All rights reserved.
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

import areca.common.base.Opt;
import areca.ui.component2.UIComposite;
import areca.ui.layout.LayoutManager;
import areca.ui.pageflow.PageflowImpl.PageHolder;

/**
 *
 * @author Falko Bräutigam
 */
public interface PageLayout {

    /**
     *
     */
    interface PageLayoutSite {

        public Opt<PageHolder> page( UIComposite pageContainer );

        public Pageflow pageflow();

        public UIComposite container();
    }

    public LayoutManager manager();

}
