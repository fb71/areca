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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import areca.common.Assert;
import areca.common.event.EventListener;
import areca.common.event.EventManager.EventHandlerInfo;
import areca.ui.Action;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrites;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.pageflow.Pageflow.PageBuilder;
import areca.ui.pageflow.PageflowEvent.EventType;

/**
 * {@link Page} is the basic building block of an application. A Page controls the
 * behaviour of a "window" or a dialog or a part thereof. An application is comprised
 * a number of Pages.
 * <p>
 * A Page can be implemented by extending the {@link Page} interface or by annotating
 * methods of a pojo with {@link Page.Init}, {@link Page.CreateUI}, {@link Page.Di}.
 * <p>
 * Every Page has a {@link Context} of variables. A context variable can be mutable
 * or immutable.
 *
 * @author Falko Bräutigam
 */
public abstract class Page {

    /**
     * Denotes one or more methods of a pojo page which are called after all
     * {@link Context} variables are injected and before the page is opened.
     * @see Page#init(PageSite)
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Init {}

    /**
     * Denotes one or more methods of a pojo page which are called when the page is
     * about to close. The method must return a value of type {@link Boolean} or
     * boolean.
     * @see Page#close()
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Close {}

    /**
     * Denotes one or more methods of a pojo page which are called after
     * the page is closed in order to dispose all resources.
     * @see Page#dispose()
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Dispose {}

    /**
     * @see Page#createUI(UIComposite)
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface CreateUI {}

    /**
     * Denotes a part of a page. A part may have {@link Context} variables to
     * be injected. Parts are not shared between pages. Injected instances are
     * created on demand and are unique per page and its parts.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Part {
    }


    /**
     * Denotes a context variable to be injected into a page. Context variables are
     * passed/shared between pages.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Context {
        public static String DEFAULT_SCOPE = "_default_";

        String scope() default DEFAULT_SCOPE;

        /** This context variable is required to be not null. */
        boolean required() default true;
    }

    // instance *******************************************

    protected PageSite      pageSite;


    void init( PageSite site ) {
        this.pageSite = site;
        onInit();
    }

    UIComponent createUI( UIComposite parent ) {
        var result = onCreateUI( parent );
        Assert.notSame( parent, result, "A Page must create a UIComposite." );
        result.cssClasses.add( getClass().getSimpleName() );
        return result;
    }

    boolean close() {
        return onClose();
    }

    void dispose() {
        onDispose();
        this.pageSite = null;
    }


    /**
     *
     */
    protected void onInit() {};

    /**
     * Creates and initializes the UI components of this page.
     * <p>
     * {@link UIComposite#layout()} is automatically triggered on the result.
     * This method should/must not call layout() on the resulting component.
     *
     * @see PageContainer
     * @param parent
     * @return The root of the newly created components of this Page.
     */
    protected abstract UIComponent onCreateUI( UIComposite parent );

    protected boolean onClose() {return true;};

    protected void onDispose() {};


    /**
     * The interface for the {@link Page} to communicate with the system.
     */
    public static abstract class PageSite {

        /**
         * Allows the Page to add actions to be shown in the global "toolbar".
         */
        public ReadWrites<?,Action> actions = Property.rws( this, "actions", new ArrayList<>() );

        /**
         * Prepare a {@link PageBuilder} in order to open a new page. Sets
         * this Page as {@link PageBuilder#parent(Object)} of the new page.
         *
         * @see Pageflow#create(Object)
         * @param newPage
         */
        public abstract PageBuilder createPage( Object newPage );

        /**
         * Close the Page this site belongs to.
         */
        public abstract void close();
        
        /**
         * True if the last {@link EventType} was {@link EventType#PAGE_CLOSING} or
         * {@link EventType#PAGE_CLOSED}
         */
        public abstract boolean isClosed();

        public <R> R context( Class<R> type ) {
            return context( type, Page.Context.DEFAULT_SCOPE );
        }

        /**
         * Retrieve a variable of the context of this {@link Page}.
         *
         * @param <R> The type of the context variable to retrieve.
         * @param type The {@link Class} of the context variable to retrieve.
         * @param scope
         * @return The value of the context variable, or null.
         */
        public abstract <R> R context( Class<R> type, String scope );

        public abstract EventHandlerInfo subscribe( EventType type, EventListener<PageflowEvent> l );
    }

}
