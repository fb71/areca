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
package areca.ui.form;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.GenericType.ParameterizedType;
import areca.ui.component2.UIComposite;
import areca.ui.form.UI.NO_TRANSFORMER;
import areca.ui.form.UI.NO_VALIDATOR;
import areca.ui.form.UI.NO_VIEWER;
import areca.ui.viewer.modeladapter.ModelValueBase;

/**
 * Renders a Form with {@link UI} specified fields.
 *
 * @author Falko Bräutigam
 */
public class FormRenderer {

    private static final Log LOG = LogFactory.getLog( FormRenderer.class );

    protected Form          form;


    public FormRenderer( Form form ) {
        this.form = form;
    }


    public void render( UIComposite container ) {
        for (var f : ClassInfo.of( form ).fields()) {
            f.annotation( UI.class ).ifPresent( ui -> {
                try {
                    var fb = form.newField();

                    Assert.that( f.genericType() instanceof ParameterizedType, "Form field has to be ParameterizedType: " + f.name() );
                    var type = (ParameterizedType)f.genericType();
                    Assert.that( ModelValueBase.class.isAssignableFrom( type.getRawType() ), "Form field has to be of type ModeAdapter: " + f.name() );
                    var adapter = (ModelValueBase)f.get( form );
                    Assert.notNull( adapter, "Adapter is mandatory." );
                    fb.adapter( adapter );

                    if (ui.viewer() != NO_VIEWER.class) {
                        fb.viewer( ClassInfo.of( ui.viewer() ).newInstance() );
                    }
                    if (ui.transformer() != NO_TRANSFORMER.class) {
                        fb.transformer( ClassInfo.of( ui.transformer() ).newInstance() );
                    }
                    if (ui.validator() != NO_VALIDATOR.class) {
                        fb.validator( ClassInfo.of( ui.validator() ).newInstance() );
                    }
                    container.add( fb.create() );
                }
                catch (InstantiationException|IllegalAccessException e) {
                    LOG.warn( "Unable to create field: " + f.name(), e );
                }
            });
        }
    }

}
