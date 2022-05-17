/*
 * Copyright (C) 2021-2022, the @authors. All rights reserved.
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
package areca.ui.pageflow;

import java.util.ArrayList;

import areca.common.Assert;
import areca.ui.Action;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrites;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class Page {

    protected PageSite      site;


    @SuppressWarnings("hiding")
    UIComponent init( UIComposite parent, PageSite site ) {
        this.site = site;
        return Assert.notSame( parent, doInit( parent ) );
    }

    void dispose() {
        doDispose();
        this.site = null;
    }

    protected abstract UIComponent doInit( UIComposite parent );

    protected abstract void doDispose();


    /**
     * The interface for the Page to communicate with the system.
     */
    public static abstract class PageSite {

        /** Allows the Page to add actions to be shown in its context. */
        public ReadWrites<?,Action> actions = Property.rws( this, "actions", new ArrayList<>() );

        public Pageflow pageflow() {
            return Pageflow.current();
        }

        public <R> R data( Class<R> type ) {
            return data( type, "__default__" );
        }

        public abstract <R> R data( Class<R> type, String scope );

        public PageSite put( Object data ) {
            return put( data, "__default__" );

        }
        public abstract PageSite put( Object data, String scope );
    }

}
