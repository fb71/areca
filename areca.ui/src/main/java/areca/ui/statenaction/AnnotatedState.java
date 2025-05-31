/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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
package areca.ui.statenaction;

import java.lang.annotation.Annotation;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;

/**
 *
 * @author Falko Bräutigam
 */
public class AnnotatedState
        implements State {

    private static final Log LOG = LogFactory.getLog( AnnotatedState.class );

    private Object          state;

    private ClassInfo<?>    classInfo;


    public AnnotatedState( Object state ) {
        this.state = Assert.notNull( state );
        this.classInfo = ClassInfo.of( state );
    }


    public void init() {
        invokeAction( State.Init.class );
    }


    /**
     * Does not invoke dispose action because this method call was triggered by that
     * action.
     */
    public void dispose() {
        invokeAction( State.Dispose.class );

//        for (var f : classInfo.fields()) {
//            f.annotation( State.Model.class ).ifPresent( a -> {
//                if (ModelBase.class.isAssignableFrom( f.type() )) {
//                    var modelValue = (ModelBase)f.get( state );
//                    if (modelValue != null) {
//                        modelValue.dispose();
//                    }
//                }
//            });
//        }
    }


    protected void invokeAction( Class<? extends Annotation> type ) {
        for (var m : classInfo.methods()) {
            m.annotation( type ).ifPresent( a -> {
                m.invokeThrowingRuntimeException( state );
            });
        }
        for (var f : classInfo.fields()) {
            f.annotation( type ).ifPresent( a -> {
                var action = Assert.isType( StateAction.class, f.get( state ),
                        "A @State.Action annotated field must be of type StateAction" );
                action.run();
            });
        }
    }

}
