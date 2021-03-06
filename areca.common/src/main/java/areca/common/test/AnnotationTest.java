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
package areca.common.test;

import java.util.List;
import java.util.logging.Logger;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;

import areca.common.Assert;
import areca.common.base.Sequence;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.GenericType.ClassType;
import areca.common.reflect.GenericType.ParameterizedType;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 *
 * @author falko
 */
@Test
public class AnnotationTest {

    private static final Logger LOG = Logger.getLogger( AnnotationTest.class.getSimpleName() );

    public static final AnnotationTestClassInfo info = AnnotationTestClassInfo.instance();

    public List<String>         parameterized;

    public List<List<String>>   parameterized2;

    public int                  primitive;


    @Test
    public void parameterizedFieldTest() {
        ClassType primitiveType = (ClassType)info.primitiveFieldInfo().genericType();
        Assert.that( primitiveType.getRawType().equals( Integer.TYPE ) );

        ParameterizedType parameterizedType = (ParameterizedType)info.parameterizedFieldInfo().genericType();
        Assert.that( parameterizedType.getRawType().equals( List.class ) );
        Assert.that( ((ClassType)parameterizedType.getActualTypeArguments()[0]).getRawType().equals( String.class ) );
    }


    @Test
    public void classInfoOfTest() {
        ClassInfo<RuntimeInfoTest> classInfo = ClassInfo.of( RuntimeInfoTest.class );
        Assert.notNull( classInfo );
        LOG.info( "INFO: " + classInfo.name() );
//        ClassInfo<AnnotationTest> classInfo2 = ClassInfo.of( AnnotationTest.class ).get();
//        Assert.isSame( classInfo, classInfo2 );
    }


    @Test
    public void staticFieldTest() {
        var infoField = Sequence.of( info.fields() ).filter( f -> f.name().equals( "info" ) ).single();
        Assert.isSame( info, infoField.get( null ) );
    }


    @Test
    @Skip
    protected void weakReferenceTest() throws InterruptedException {
        WeakReference<Object> ref = new WeakReference<>( new Object() );

        for (int j=0; j<10; j++) {
            Object o = null;
            for (int i=0; i<50000000; i++) {
                o = String.valueOf( i );
            }
            LOG.info( "" + (j) + "   " + o + " :: "+ ref.get() );
            Thread.sleep( 1000 );
            System.gc();
        }

        LOG.info( "!" );
        Thread.sleep( 5000 );
        LOG.info( "" + ref.get() );
    }


//    @Test
//    public void declaredSuperTest() {
//        System.out.println( "    " + "declaredSuperTest()" );
//        LOG.info( AnnotationTestClassInfo.INFO.declaredSuperTestMethodInfo().name() + " ..." );
//        for (Class<?> cl=Annotated.class; cl!=null; cl=cl.getSuperclass()) {
//
//        }
//    }


    /** */
//    @Annotation1
    static class Annotated
            extends AnnotatedBase
            implements AnnotatedInterface {

//        @Annotation1("s1")
        private String      s1;

//        @Annotation1
        public void m1() {
        }
    }


    static class AnnotatedBase {
    }

    static interface AnnotatedInterface {
    }


    @Documented
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Annotation1 {
        String value() default "_default_";
    }

}
