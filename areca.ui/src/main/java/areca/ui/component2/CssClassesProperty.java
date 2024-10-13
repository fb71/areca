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
package areca.ui.component2;

import static areca.ui.component2.UIComponent.PROP_CSS_CLASSES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class CssClassesProperty
        extends Property.ReadWrites<UIComponent,String> {

    private static final Log LOG = LogFactory.getLog( CssClassesProperty.class );

    /** Permanent CSS classes for theming */
    private Collection<String>  themeClasses = Collections.EMPTY_LIST;

    protected CssClassesProperty( UIComponent component ) {
        super( component, PROP_CSS_CLASSES, new ArrayList<>() );
    }

    public void setThemeClasses( String... themeClasses ) {
        this.themeClasses = Arrays.asList( themeClasses );
        fireEvent( null, value );
    }

    public void setThemeClasses( Collection<String> themeClasses ) {
        this.themeClasses = themeClasses;
        fireEvent( null, value );
    }

    public void addThemeClass( String themeClass ) {
        this.themeClasses.add( themeClass );
        fireEvent( null, value );
    }

    public void removeThemeClass( String themeClass ) {
        this.themeClasses.remove( themeClass );
        fireEvent( null, value );
    }


    @Override
    protected void fireEvent( Collection<String> oldValue, Collection<String> newValue ) {
        var combined = Sequence.of( newValue ).concat( themeClasses ).asCollection();
        //LOG.info( "%s -> %s", component.getClass().getSimpleName(), combined );
        super.fireEvent( null, combined );
    }

}