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
package areca.ui.form;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import areca.ui.component2.UIComponent;
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
    public class NO_VIEWER extends Viewer {
        @Override public UIComponent create() { throw new UnsupportedOperationException(); }
        @Override public void store() { throw new UnsupportedOperationException(); }
        @Override public void load() { throw new UnsupportedOperationException(); }
        @Override protected boolean isDisposed() { throw new UnsupportedOperationException(); }
    }

    // interface ******************************************

    public String label() default NO_LABEL;

    //public Class<?> type();

    @SuppressWarnings("rawtypes")
    public Class<? extends Viewer> viewer() default NO_VIEWER.class;

    //public Class<? extends ModelAdapter> adapter(); // default ModelAdapter.class;

}
