/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.ui.controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import areca.ui.component.UIComponent;
import areca.ui.component.UIComposite;
import areca.ui.viewer.ModelAdapter;
import areca.ui.viewer.ModelValueTransformer;
import areca.ui.viewer.Viewer;

/**
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface UI {

    public static final String  NO_LABEL = "__no_label__";

    @SuppressWarnings("rawtypes")
    public class DEFAULT_VIEWER extends Viewer {
        @Override public UIComponent create( UIComposite container ) { throw new UnsupportedOperationException(); }
    }

    @SuppressWarnings("rawtypes")
    public class DEFAULT_TRANSFORMER implements ModelValueTransformer {
        @Override public Object transform2UI( Object value ) { throw new UnsupportedOperationException(); }
    }

    // interface ******************************************

    public String label() default NO_LABEL;

    public Class<?> type();

    @SuppressWarnings("rawtypes")
    public Class<? extends Viewer> viewer() default DEFAULT_VIEWER.class;

    public Class<? extends ModelAdapter> adapter() default ModelAdapter.class;

    @SuppressWarnings("rawtypes")
    public Class<? extends ModelValueTransformer> transformer() default DEFAULT_TRANSFORMER.class;
}
