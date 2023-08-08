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
import areca.ui.Action;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrites;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.pageflow.Pageflow.PageBuilder;

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
     *
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Part {
    }

    /**
     * Denotes one or more methods of a pojo page which are called after all
     * {@link Context} variables are injected and before the page is opened.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Init {}

    /**
     * Denotes one or more methods of a pojo page which are called when the page is
     * about to close. The method must return a value of type {@link Boolean} or
     * boolean.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Close {}

    /**
     * Denotes one or more methods of a pojo page which are called after
     * the page is closed in order to dispose all resources.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Dispose {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface CreateUI {}

    /**
     * Denotes a context variable to be injected into a page.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Context {
        public static String DEFAULT_SCOPE = "_default_";
//        public enum Mode {
//            /** */
//            SHADE,
//            /** */
//            ADD
//        }
        String scope() default DEFAULT_SCOPE;
//        Mode mode() default Mode.SHADE;
    }

//    /**
//     * A modifiable {@link Context} variable.
//     *
//     * @param <T>
//     */
//    public static class ContextVar<T> {
//
//        Page.PageSite   pageSite;
//
//        ContextVar( T value ) {
//            //this.value = value;
//        }
//
//        public T get() {
//            return pageSite.context( )
//        }
//
//        public void set( T value ) {
//            this.value = value;
//        }
//    }

//    /**
//     *
//     */
//    public static class Scoped {
//        public enum Mode {
//            SHADE,
//            ADD
//        }
//        public static Scoped $( Object _value ) {
//            return new Scoped() {{value = _value;}};
//        }
//        public static Scoped $( Object _value, Mode _mode ) {
//            return new Scoped() {{value = _value; mode = _mode;}};
//        }
//        public static Scoped $( Object _value, String _scope ) {
//            return new Scoped() {{value = _value; this.scope = _scope;}};
//        }
//        Object      value;
//        Mode        mode = Mode.ADD;
//        String      scope = Page.Context.DEFAULT_SCOPE;
//    }

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

        public <R> R context( Class<R> type ) {
            return context( type, Page.Context.DEFAULT_SCOPE );
        }

        public abstract <R> R context( Class<R> type, String scope );
    }

}
