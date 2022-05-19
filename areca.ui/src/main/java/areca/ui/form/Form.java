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

/**
 *
 * @author Falko Bräutigam
 */
public class Form {

//    protected Site          site;
//
//
//    protected void init( @SuppressWarnings("hiding") Site site ) {
//        this.site = site;
//    }
//
//
//    protected void customize( UIComposite container ) {
//    }
//
//
//    protected void dispose() {
//    }
//
//
//    protected EventHandlerInfo subscribeUIEvent( EventListener<?> l ) {
//        throw new RuntimeException("not yet...");
//    }
//
//
//    protected boolean isSource( EventObject ev, ModelAdapter modelAdapter ) {
//        throw new RuntimeException("not yet...");
//    }

    public FieldBuilder newField() {
        return new FieldBuilder();
    }

    public void submit() {
        throw new RuntimeException( "not yet..." );
    }

    public void revert() {
        throw new RuntimeException( "not yet..." );
    }

//    /**
//     *
//     */
//    public interface Site {
//
//        public FieldBuilder newField();
//
//        //public void addContext( Object value, Mode mode, String scope );
//
//    }

}
