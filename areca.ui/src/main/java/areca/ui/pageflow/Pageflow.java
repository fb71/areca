/*
 * Copyright (C) 2021-2023, the @authors. All rights reserved.
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

import areca.common.Assert;
import areca.common.Session;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.component2.UIComposite;

/**
 * The API of the Pageflow system.
 *
 * @author Falko Bräutigam
 */
public abstract class Pageflow {

    private static final Log LOG = LogFactory.getLog( Pageflow.class );

    public static Pageflow start( UIComposite rootContainer ) {
        return Session.setInstance( new PageflowImpl( rootContainer ) );
    }

    public static Pageflow current() {
        return Session.instanceOf( PageflowImpl.class );
    }

    /**
     *
     */
    protected static class Scoped {

        String      scope;

        Object      value;

        public Scoped( Object value, String scope ) {
            this.scope = Assert.notNull( scope, "Scope must not be null. Use DEFAULT_SCOPE instead." );
            this.value = Assert.notNull( value, "Value of a context variable must not be null. Removing is not yet supported." );
        }

        public boolean isCompatible( Class<?> type, @SuppressWarnings("hiding") String scope ) {
            return type.isAssignableFrom( value.getClass() ) && scope.equals( this.scope );
        }
    }


    // instance *******************************************

    public abstract void dispose();

    public abstract boolean isDisposed();


    /**
     * Prepares a new page to {@link PageBuilder#open()}. By default the new page is
     * opened on top of the current page stack. Change this via
     * {@link PageBuilder#parent(Object)}
     *
     * @param page An instance of {@link Page} or an annotated object that acts as
     *        the controller of the newly created page.
     */
    public abstract PageBuilder create( Object page );


    /**
     * API for client code to create/open a new Page.
     */
    public interface PageBuilder {

        public PageBuilder origin( Position origin );

        public PageBuilder parent( Object parent );

        public PageBuilder putContext( Object value, String scope );

        /**
         * Actually opens the newly created page in the UI.
         */
        public void open();
    }


    public abstract void close( Object page );

    /**
     * True if the given page currently exists and is open.
     */
    public abstract boolean isOpen( Object page );


    /**
     * The sequence of pages in this Pageflow.
     */
    public abstract Sequence<Object,RuntimeException> pages();


    public abstract Object topPage();
}
