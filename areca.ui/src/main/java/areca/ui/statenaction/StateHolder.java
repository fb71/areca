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
import areca.common.event.EventManager;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.GenericType.ClassType;
import areca.common.reflect.GenericType.ParameterizedType;
import areca.ui.modeladapter.ModelValue;
import areca.ui.statenaction.StateChangeEvent.EventType;

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
        StateChangeEvent.publish( EventType.INITIALIZED, state );
    }


    @Override
    public void dispose() {
        disposed = true;
        new AnnotatedState( state ).dispose();
        StateChangeEvent.publish2( EventType.DISPOSED, state )
                .onSuccess( __ -> disposeEventDelivered = true );
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
            public void activate() {
                result.init();
            }
        };
    }



    @SuppressWarnings("unchecked")
    protected void injectContext( Object part ) {
        for (var f : ClassInfo.of( part ).fields()) {
            // @State.Context
            f.annotation( State.Context.class ).ifPresent( a -> {
                var value = (Object)null;
                // ModelValue
                if (ModelValue.class.isAssignableFrom( f.type() )) {
                    var type = (ParameterizedType)f.genericType();
                    var typeArg = ((ClassType)type.getActualTypeArguments()[0]).getRawType();
                    value = context.entry( typeArg, a.scope() ).orNull();
                    if (value == null && a.required()) {
                        throw new IllegalStateException( "Context variable of type: '" + f.type().getSimpleName()
                                + "' required but absent in context of: " + part.getClass().getSimpleName() );
                    }
                    var modelValue = ModelValue.class.cast( f.get( part ) );
                    modelValue.set( value );
                }
                // simple field
                else {
                    value = context.entry( (Class<?>)f.type(), a.scope() ).orNull();
                    if (value == null && a.required()) {
                        throw new IllegalStateException( "Context variable of type: '" + f.type().getSimpleName()
                                + "' required but absent in context of: " + part.getClass().getSimpleName() );
                    }
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
