/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.common.reflect;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import areca.common.Assert;
import areca.common.base.Lazy.RLazy;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Runtime information of a class. Similar to the Java Class object.
 *
 * @author Falko Bräutigam
 */
public abstract class ClassInfo<T>
        implements Named, Annotated, Typed {

    private static final Log LOG = LogFactory.getLog( ClassInfo.class );

    private static Map<Class<?>,ClassInfo<?>>  classInfos = new /*Concurrent*/HashMap<>( 128 );


    @SuppressWarnings({"unchecked", "deprecation"})
    public static <R> ClassInfo<R> of( Class<R> cl ) {
        ClassInfo<R> result = (ClassInfo<R>)classInfos.get( cl );
        if (result == null) {
            try {
                // this is (teavm?) magic; it forces the particular ClassInfo class to
                // be fully (!?) loaded -> instance constant initialized -> classInfo
                // registered
                cl.newInstance();
                result = (ClassInfo<R>)classInfos.get( cl );
            }
            catch (InstantiationException | IllegalAccessException e) {
            }
        }
        if (result == null) {
            throw new IllegalStateException( "ClassInfo: not found for: " + cl.getName() + " (Either not generated or not yet loaded. Static constant in class missing??)" );
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    public static <R> ClassInfo<R> of( R o ) {
        Assert.notNull( o );
        return of( (Class<R>)o.getClass() );
    }


    // instance *******************************************

    private RLazy<List<MethodInfo>> declaredMethods = new RLazy<>( () -> createDeclaredMethods() );

    private RLazy<List<AnnotationInfo>> annotations = new RLazy<>( () -> createAnnotations() );

    private RLazy<List<FieldInfo>> fields = new RLazy<>( () -> createFields() );


    protected ClassInfo() {
        // either literal access via instance variable or via of() static method.
        classInfos.put( type(), this );
        LOG.info( "init: %s", type().getName() );
    }

    @Override
    public int hashCode() {
        return type().hashCode();
    }

    @Override
    public boolean equals( Object other ) {
        if (other instanceof ClassInfo) {
            return type().equals( ((Typed)other).type() );
        }
        return false;
    }

    public abstract String name();

    public abstract String simpleName();

    public abstract Class<T> type();

    /**
     * Information of the superclass.
     *
     * @return The result is absent if the superclass is {@link Object} <b>OR</b> if
     * the superclass is not annotated and hence no runtime information are available.
     */
    public abstract Opt<ClassInfo<?>> superclassInfo();

    public abstract T newInstance() throws InstantiationException, IllegalAccessException;


    @Override
    public List<AnnotationInfo> annotations() {
        return annotations.supply();
    }

    protected abstract List<AnnotationInfo> createAnnotations();


    /**
     * All methods declared by this class and {@link #superclassInfo() annotated
     * super classes}.
     * <p>
     * TODO Current implementation does not properly support method signatures. There
     * is always just one method for any given name in the result.
     */
    public Collection<MethodInfo> methods() {
        Map<String,MethodInfo> result = new HashMap<>( 128 );
        ClassInfo<?> ci = this;
        while (ci != null) {
            for (MethodInfo m : ci.declaredMethods()) {
                if (result.putIfAbsent( m.name(), m ) != null) { // TODO check complete signature
                    LOG.debug( "Method overridden: %s::%s", ci.name(), m.name() );
                }
            }
            ci = ci.superclassInfo().orElse( null );
        }
        return result.values();
    }


    /**
     * The methods declared by this class.
     */
    public Collection<MethodInfo> declaredMethods() {
        return declaredMethods.supply();
    }


    protected abstract List<MethodInfo> createDeclaredMethods();


    public List<FieldInfo> fields() {
        return fields.supply();
    }

    protected abstract List<FieldInfo> createFields();

}
