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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import areca.common.Assert;
import areca.common.base.Lazy;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class ClassInfo<T>
        implements Named, Annotated, Typed {

    private static final Logger LOG = Logger.getLogger( ClassInfo.class.getName() );

    private static Map<Class<?>,ClassInfo<?>>  classInfos = new /*Concurrent*/HashMap<>( 128 );


    @SuppressWarnings({"unchecked"})
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

    private Lazy<List<MethodInfo>,RuntimeException> methods = new Lazy<>( () -> createMethods() );

    private Lazy<List<AnnotationInfo>,RuntimeException> annotations = new Lazy<>( () -> createAnnotations() );

    private Lazy<List<FieldInfo>,RuntimeException> fields = new Lazy<>( () -> createFields() );


    protected ClassInfo() {
        // either literal access via instance variable or via of() static method.
        classInfos.put( type(), this );
        LOG.info( "ClassInfo: " + type().getName() );
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

    public abstract T newInstance() throws InstantiationException, IllegalAccessException;


    @Override
    public List<AnnotationInfo> annotations() {
        return annotations.supply();
    }

    protected abstract List<AnnotationInfo> createAnnotations();


    public List<MethodInfo> methods() {
        return methods.supply();
    }

    protected abstract List<MethodInfo> createMethods();


    public List<FieldInfo> fields() {
        return fields.supply();
    }

    protected abstract List<FieldInfo> createFields();

}
