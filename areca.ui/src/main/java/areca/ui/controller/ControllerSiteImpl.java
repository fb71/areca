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

import java.util.ArrayList;
import java.util.List;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.FieldInfo;
import areca.ui.component.UIComposite;
import areca.ui.controller.Context.Mode;
import areca.ui.viewer.ModelAdapter;

/**
 *
 * @author Falko Bräutigam
 */
public class ControllerSiteImpl
        implements Controller.Site {

    private static final Log log = LogFactory.getLog( ControllerSiteImpl.class );

    public static class ContextValue {
        public String       scope;
        public Mode         mode;
        public Object       value;

        public boolean isCompatible() {
            throw new RuntimeException("testing...");
        }
    }

    // instance *******************************************

    protected Controller            controller;

    protected List<ContextValue>    contextValues = new ArrayList<>();


    public ControllerSiteImpl( Controller controller ) {
        this.controller = controller;
        injectContextFields();
        controller.init( this );
    }


    protected void injectContextFields() {
        for (FieldInfo f : ClassInfo.of( controller.getClass() ).fields()) {
            f.annotation( Context.class ).ifPresent( a -> {
                if (a.mode() == Mode.IN || a.mode() == Mode.IN_OUT) {
                    // TODO ask parent
                    throw new RuntimeException( "not yet implemented." );
                }
                else if (a.mode() == Mode.LOCAL || a.mode() == Mode.OUT) {
                    new ContextValue() {{mode = a.mode();}};
                }
            });
        }
    }


    public UIComposite createUI( UIComposite container ) {
        // fields that are added by the controller or outside caller
        injectContextFields();

        for (FieldInfo f : ClassInfo.of( controller.getClass() ).fields()) {
            f.annotation( UI.class ).ifPresent( a -> {
                try {
                    FieldBuilder fb = new FieldBuilder();
                    if (!a.label().equals( UI.NO_LABEL )) {
                        fb.label( a.label() );
                    }
                    if (!a.viewer().equals( UI.DEFAULT_VIEWER.class )) {
                        fb.viewer( a.viewer().newInstance() );
                    }
                    if (!a.adapter().equals( ModelAdapter.class )) {
                        fb.adapter( a.adapter().newInstance() );
                    }
                    if (!a.transformer().equals( UI.DEFAULT_TRANSFORMER.class )) {
                        fb.transformer( a.transformer().newInstance() );
                    }
                    fb.create( container );
                }
                catch (Exception e) {
                    throw new RuntimeException( e );
                }
            });
        }
        return container;
    }



    @Override
    public FieldBuilder newField() {
        return new FieldBuilder();
    }


    @Override
    public void addContext( Object value, Mode mode, String scope ) {
        // TODO
        throw new RuntimeException( "not yet implemented." );
    }

}
