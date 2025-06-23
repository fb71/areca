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

import areca.common.Assert;
import areca.common.base.Opt;
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.GenericType.ClassType;
import areca.common.reflect.GenericType.ParameterizedType;
import areca.ui.statenaction.StateChangeEvent.EventType;
import areca.ui.viewer.model.Model;

/**
 * The runtime/site of a State.
 *
 * @author Falko Bräutigam
 */
class StateHolder
        implements StateSite {

    private static final Log LOG = LogFactory.getLog( StateHolder.class );

    private Object          state;

    private StateHolder     parent;

    private StateHolder     child;

    private ContextVariables context;

    private boolean         disposed;

    /**  */
    private boolean         disposeEventDelivered;

    /**
     * StateHolder for a root/start State, with no State and no parent.
     */
    public StateHolder() {
        this.context = new ContextVariables();
    }


    public StateHolder( Object state, StateHolder parent ) {
        this.state = Assert.notNull( state );
        this.parent = Assert.notNull( parent );
        this.context = new ContextVariables( parent.context );
        context.put( StateHolder.this, State.Context.DEFAULT_SCOPE );
    }


    public void init() {
        injectContext( state );
        new AnnotatedState( state ).init();
        StateChangeEvent.publish( EventType.INITIALIZED, state, this );
    }


    @Override
    public void dispose() {
        if (!disposed) {
            if (child != null) {
                child.dispose();
            }
            disposed = true;
            parent.child = null;
            new AnnotatedState( state ).dispose();
            StateChangeEvent.publish2( EventType.DISPOSED, state, this )
                    .onSuccess( __ -> disposeEventDelivered = true );
        }
    }


    @Override
    public boolean isDisposed() {
        return disposed;
    }


    @Override
    public StateBuilder createState( Object newState ) {
        return new StateBuilder() {

            private StateHolder     result = new StateHolder( newState, StateHolder.this );

            @Override
            public StateBuilder putContext( Object value, String scope ) {
                result.context.put( value, scope );
                return this;
            }

            @Override
            public StateBuilder onChange( Object annotatedOrListener ) {
                EventManager.instance().subscribe( annotatedOrListener )
                        .performIf( StateChangeEvent.class, ev -> ev.getSource() == result.state )
                        .unsubscribeIf( () -> result.disposeEventDelivered );
                return this;
            }

            @Override
            @SuppressWarnings( "unchecked" )
            public <R> R activate() {
                if (child != null) {
                    child.dispose();
                }
                child = result;
                result.init();
                return (R)result.state;
            }
        };
    }


    @Override
    public <R> Opt<R> opt( Class<R> type, String scope ) {
        return context.entry( type, scope );
    }


    @SuppressWarnings("unchecked")
    protected void injectContext( Object part ) {
        for (var f : ClassInfo.of( part ).fields()) {
            // @State.Context
            f.annotation( State.Context.class ).ifPresent( a -> {
                var value = (Object)null;
                // Model
                if (Model.class.isAssignableFrom( f.type() )) {
                    var type = (ParameterizedType)f.genericType();
                    var typeArg = ((ClassType)type.getActualTypeArguments()[0]).getRawType();
                    value = context.entry( typeArg, a.scope() ).orNull();
                    var modelValue = Model.class.cast( f.get( part ) );
                    Assert.that( value != null || !a.required() || modelValue.get() != null,
                            "Context variable " + f.name() + "(" + typeArg + ") required but type not found context of: " + part.getClass().getSimpleName() );
                    Assert.that( modelValue.get() == null || a.mutable(),
                            "Context variable already set (" + modelValue.get() + ") but not mutable in context of: " + part.getClass().getSimpleName() );
                    modelValue.set( value );
                }
                // simple field
                else {
                    value = context.entry( (Class<?>)f.type(), a.scope() ).orNull();
                    Assert.that( value != null || !a.required() || f.get( part ) != null,
                            "Context variable of type: '" + f.type().getSimpleName() + "' required but absent in context of: " + part.getClass().getSimpleName() );
                    Assert.that( f.get( part ) == null || a.mutable(),
                            "Context variable already set (" + f.get( part ) + ") but not mutable in context of: " + part.getClass().getSimpleName() );
                    f.set( part, f.type().cast( value ) );
                }
                LOG.debug( "inject: %s = %s", f.name(), value != null ? value : "null" );
            });
//            // Part
//            f.annotation( State.Part.class ).ifPresent( a -> {
//                try {
//                    var value = Sequence.of( parts )
//                            .first( p -> f.type().isInstance( p ) )
//                            .orElse( () -> {
//                                @SuppressWarnings("deprecation")
//                                var result = f.type().newInstance();
//                                doInject( result, valueSupplier );
//                                parts.add( result );
//                                return result;
//                            });
//                    f.set( part, value );
//                }
//                catch (ReflectiveOperationException e) {
//                    LOG.warn( "Error while trying to create an instance of: " + f.type(), e );
//                    throw new RuntimeException( e );
//                }
//            });
        }
    }

}
