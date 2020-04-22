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

import java.util.logging.Logger;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;

import areca.common.testrunner.Test;

/**
 *
 * @author falko
 */
@Test("t1")
public class AnnotationTest {

    private static final Logger LOG = Logger.getLogger( AnnotationTest.class.getSimpleName() );

    public static final AnnotationTestClassInfo INFO = AnnotationTestClassInfo.INFO;


    @Test
    protected void weakReferenceTest() throws InterruptedException {
        WeakReference ref = new WeakReference<>( new Object() );

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


    @Test("t2")
    public void declaredSuperTest() {
        System.out.println( "    " + "declaredSuperTest()" );
        LOG.info( INFO.declaredSuperTestMethodInfo().name() + " ..." );
        for (Class cl=Annotated.class; cl!=null; cl=cl.getSuperclass()) {

        }
    }


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
