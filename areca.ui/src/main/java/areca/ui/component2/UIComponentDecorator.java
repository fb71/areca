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
package areca.ui.component2;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;
import areca.ui.component2.UIComponentEvent.DecoratorAttachedEvent;
import areca.ui.component2.UIComponentEvent.DecoratorDetachedEvent;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class UIComponentDecorator
        extends UIElement {

    private static final Log LOG = LogFactory.getLog( UIComponentDecorator.class );

    private UIComponent         decorated;


    protected UIComponentDecorator() {
        UIComponentEvent.manager().publish( new ComponentConstructedEvent( this ) );
    }


    public UIComponent decorated() {
        return decorated;
    }


    @Override
    public void dispose() {
        if (decorated != null) {
            decorated.decorators.remove( this );
            Assert.isNull( decorated );
        }
        super.dispose();
    }


    protected void decoratorAttachedTo( UIComponent newDecorated ) {
        Assert.isNull( decorated, "This decorator is attached to another UIComponent." );
        this.decorated = Assert.notNull( newDecorated  );
        UIComponentEvent.manager().publish( new DecoratorAttachedEvent( this, newDecorated ) );
    }


    protected void decoratorDetachedFrom( UIComponent component ) {
        Assert.isSame( decorated, component, "This decorator is not attached to this UIComponent." );
        this.decorated = null;
        UIComponentEvent.manager().publish( new DecoratorDetachedEvent( this, component ) );
    }

}
