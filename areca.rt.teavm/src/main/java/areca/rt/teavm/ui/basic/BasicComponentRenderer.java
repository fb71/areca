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
package areca.rt.teavm.ui.basic;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.RuntimeInfo;
import areca.rt.teavm.ui.BadgeRenderer;
import areca.rt.teavm.ui.ButtonRenderer;
import areca.rt.teavm.ui.ColorPickerRenderer;
import areca.rt.teavm.ui.FileUploadRenderer;
import areca.rt.teavm.ui.IFrameRenderer;
import areca.rt.teavm.ui.ImageRenderer;
import areca.rt.teavm.ui.LabelRenderer;
import areca.rt.teavm.ui.LinkRenderer;
import areca.rt.teavm.ui.ProgressRenderer;
import areca.rt.teavm.ui.ScrollableCompositeRenderer;
import areca.rt.teavm.ui.SeparatorRenderer;
import areca.rt.teavm.ui.TagRenderer;
import areca.rt.teavm.ui.TextFieldRenderer;
import areca.rt.teavm.ui.TextRenderer;
import areca.rt.teavm.ui.UIComponentRenderer;
import areca.rt.teavm.ui.UICompositeRenderer;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class BasicComponentRenderer {

    private static final Log LOG = LogFactory.getLog( BasicComponentRenderer.class );

    /**
     * Starts all component renderes of this package.
     */
    public static void start() {
        UICompositeRenderer._start();
        ScrollableCompositeRenderer._start(); // after UIComposite
        TextRenderer._start();
        TextFieldRenderer._start();
        ButtonRenderer._start();
        ProgressRenderer._start();
        LinkRenderer._start();
        SeparatorRenderer._start();
        ColorPickerRenderer._start();
        FileUploadRenderer._start();
        ImageRenderer._start();
        IFrameRenderer._start();

        BadgeRenderer._start();
        LabelRenderer._start();
        TagRenderer._start();
        UIComponentRenderer._start(); // last

        BasicTheme.start();
    }

}
