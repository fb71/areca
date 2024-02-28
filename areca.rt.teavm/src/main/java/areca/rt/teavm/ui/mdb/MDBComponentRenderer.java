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
package areca.rt.teavm.ui.mdb;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.RuntimeInfo;
import areca.rt.teavm.ui.BadgeRenderer;
import areca.rt.teavm.ui.ButtonRenderer;
import areca.rt.teavm.ui.LabelRenderer;
import areca.rt.teavm.ui.LinkRenderer;
import areca.rt.teavm.ui.ProgressRenderer;
import areca.rt.teavm.ui.ScrollableCompositeRenderer;
import areca.rt.teavm.ui.SeparatorRenderer;
import areca.rt.teavm.ui.TagRenderer;
import areca.rt.teavm.ui.TextRenderer;
import areca.rt.teavm.ui.UIComponentRenderer;
import areca.rt.teavm.ui.UICompositeRenderer;
import areca.ui.component2.UIComposite;

/**
 * Starts everything to render UI in MDB style.
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class MDBComponentRenderer {

    private static final Log LOG = LogFactory.getLog( MDBComponentRenderer.class );

    /**
     * Starts everything to render UI in MDB style.
     */
    public static void start( UIComposite rootWindow ) {
        UICompositeRenderer._start();
        ScrollableCompositeRenderer._start(); // after UIComposite
        TextRenderer._start();
        TextFieldRenderer._start();
        ButtonRenderer._start();
        ProgressRenderer._start();
        LinkRenderer._start();
        SeparatorRenderer._start();

        BadgeRenderer._start();
        LabelRenderer._start();
        TagRenderer._start();
        UIComponentRenderer._start(); // last

        MDBTheme._start( rootWindow );
    }

}
