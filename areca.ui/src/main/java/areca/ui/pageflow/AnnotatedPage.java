/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
import java.util.List;

import org.apache.commons.lang3.mutable.MutableObject;

import areca.common.Assert;
import areca.common.base.BiFunction;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.FieldInfo;
import areca.common.reflect.MethodInfo;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;

/**
 * Provides a {@link Page} implementation on top of an annotated Pojo
 * (supplied by client code).
 *
 * @author Falko Bräutigam
 */
class AnnotatedPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( AnnotatedPage.class );

    public interface ContextValueSupplier<T> extends BiFunction<Class<T>,String,T,RuntimeException> {
    }

    // instance *******************************************

    /** The Pojo that implements the Page */
    private Object              delegate;

    private Pageflow            pageflow;

    private ClassInfo<Object>   pageInfo;

    private List<Object>        parts = new ArrayList<>();

    public AnnotatedPage( Object delegate, Pageflow pageflow ) {
        this.delegate = Assert.notNull( delegate );
        this.pageInfo = Assert.notNull( ClassInfo.of( delegate ) );
        this.pageflow = Assert.notNull( pageflow );
        parts.add( delegate );
    }

    public void inject( ContextValueSupplier<?> valueSupplier ) {
        doInject( delegate, valueSupplier );
    }

    protected void doInject( Object part, ContextValueSupplier<?> valueSupplier ) {
        for (FieldInfo f : ClassInfo.of( part ).fields()) {
            // Context
            f.annotation( Page.Context.class ).ifPresent( a -> {
                @SuppressWarnings({"unchecked", "rawtypes"})
                var value = valueSupplier.apply( (Class)f.type(), a.scope() );
                if (value == null && a.required()) {
                    throw new IllegalStateException( "Context variable of type: '" + f.type().getSimpleName()
                            + "' required but absent in Page: " + delegate.getClass().getSimpleName() );
                }
                f.set( part, f.type().cast( value ) );
                LOG.debug( "inject: %s = %s", f.name(), value != null ? value : "null" );
            });
            // Part
            f.annotation( Page.Part.class ).ifPresent( a -> {
                try {
                    var value = Sequence.of( parts )
                            .first( p -> f.type().isInstance( p ) )
                            .orElse( () -> {
                                @SuppressWarnings("deprecation")
                                var result = f.type().newInstance();
                                doInject( result, valueSupplier );
                                parts.add( result );
                                return result;
                            });
                    f.set( part, value );
                }
                catch (ReflectiveOperationException e) {
                    LOG.warn( "Error while trying to create an instance of: " + f.type(), e );
                    throw new RuntimeException( e );
                }
            });
        }
    }

    @Override
    protected void onInit() {
        for (MethodInfo m : pageInfo.methods()) {
            m.annotation( Page.Init.class ).ifPresent( a -> {
                m.invokeThrowingRuntimeException( delegate );
            });
        }
    }

    @Override
    protected UIComponent onCreateUI( UIComposite parent ) {
        var result = new MutableObject<UIComponent>();
        for (MethodInfo m : pageInfo.methods()) {
            m.annotation( Page.CreateUI.class ).ifPresent( a -> {
                Assert.isNull( result.getValue(), "More than on method annotated with @Page.CreateUI!" );
                result.setValue( Assert.isType( UIComponent.class, m.invokeThrowingRuntimeException( delegate, parent ) ) );
            });
        }
        return Assert.notNull( result.getValue(), "No method found with annotation @Page.CreateUI" );
    }

    @Override
    protected void onDispose() {
        for (MethodInfo m : pageInfo.methods()) {
            m.annotation( Page.Dispose.class ).ifPresent( a -> {
                m.invokeThrowingRuntimeException( delegate );
            });
        }
    }

    @Override
    protected boolean onClose() {
        for (MethodInfo m : pageInfo.methods()) {
            if (m.annotation( Page.Close.class ).isPresent()) {
                return (Boolean)m.invokeThrowingRuntimeException( delegate );
            }
        }
        return true;
    }

}
