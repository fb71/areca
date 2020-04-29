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
package areca.common.testrunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import java.lang.reflect.InvocationTargetException;

import areca.common.AssertionException;
import areca.common.reflect.AnnotationInfo;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.MethodInfo;

/**
 * Very simple test framework.
 * <p>
 * JUnit is not an option as it might not be supported by all possible runtimes.
 *
 * @author <a href="http://polymap.de">Falko Bräutigam</a>
 */
public class TestRunner {

    private static final Object[] NOARGS = new Object[] {};

    // instrance ******************************************

    private List<ClassInfo<?>>                  testTypes = new ArrayList<>( 128 );

    private List<ClassInfo<? extends TestRunnerDecorator>> decoratorTypes = new ArrayList<>( 128 );

    private List<TestResult>                    testResults = new ArrayList<>( 128 );


    public TestRunner addTests( ClassInfo<?>... tests ) {
        testTypes.addAll( Arrays.asList( tests ) );
        return this;
    }


    @SuppressWarnings("unchecked")
    public TestRunner addDecorators( ClassInfo<? extends TestRunnerDecorator>... decorators ) {
        decoratorTypes.addAll( Arrays.asList( decorators ) );
        return this;
    }


    public void run() {
        List<TestRunnerDecorator> decorators = decoratorTypes.stream().map( cl -> instantiate( cl ) ).collect( Collectors.toList() );

        // all test classes
        decorators.forEach( d -> d.preRun( this ) );
        for (ClassInfo<?> cl : testTypes) {

            // Before/After
            List<MethodInfo> befores = cl.methods().stream()
                    .filter( m -> m.annotation( BeforeAnnotationInfo.INFO ).isPresent() )
                    .collect( Collectors.toList() );
            List<MethodInfo> afters = cl.methods().stream()
                    .filter( m -> m.annotation( AfterAnnotationInfo.INFO ).isPresent() )
                    .collect( Collectors.toList() );

            // all test methods
            decorators.forEach( d -> d.preTest( cl ) );
            for (TestMethod m : findTestMethods( cl )) {
                decorators.forEach( d -> d.preTestMethod( m ) );

                // method
                TestResult testResult = new TestResult( m );
                testResults.add( testResult );
                Class<? extends Throwable> expected = m.m.annotation( TestAnnotationInfo.INFO ).get().expected();
                try {
                    Object test = instantiate( cl );
                    for (MethodInfo before : befores) {
                        before.invoke( test, NOARGS );
                    }
                    m.m.invoke( test, NOARGS );
                    if (!expected.equals( Test.NoException.class )) {
                        testResult.setException( new AssertionException( "Exception expected: " + expected.getName() ) );
                    }
                    for (MethodInfo after : afters) {
                        after.invoke( test, NOARGS );
                    }
                }
                catch (InvocationTargetException e ) {
                    if (expected.equals( Test.NoException.class )
                            || !expected.isAssignableFrom( e.getCause().getClass() )) {
                        testResult.setException( e.getCause() );
                    }
                }
                catch (Throwable e ) {
                    testResult.setException( e );
                }
                finally {
                    testResult.done();
                }
                decorators.forEach( d -> d.postTestMethod( m, testResult ) );
            }
            decorators.forEach( d -> d.postTest( cl ) );
        }
        decorators.forEach( d -> d.postRun( this ) );
    }


    protected <R> R instantiate( ClassInfo<R> cl ) {
        try {
            return cl.newInstance();
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
            System.out.println( e );
            throw new RuntimeException( e );
        }
    }


    protected List<TestMethod> findTestMethods( ClassInfo<?> cl ) {
        //System.out.println( "TEST METHODS  of: " + cl.name() + " (" + cl.methods().size() + ")" );
        return cl.methods().stream()
                //.peek( m -> System.out.println( "    " + m ) )
                .filter( m -> m.annotation( TestAnnotationInfo.INFO ).isPresent())
                //.peek( m -> System.out.println( "    " + m ) )
                .map( m -> new TestMethod( m ) )
                .collect( Collectors.toList() );
    }


    /**
     *
     */
    public static class TestMethod {
        private MethodInfo      m;
        private AnnotationInfo  a;

        public TestMethod( MethodInfo m ) {
            this.m = m;
            this.a = m.annotation( TestAnnotationInfo.INFO ).get();
        }

        public String name() {
            return m.name();
        }
    }


    /**
     *
     */
    public static class TestResult {
        private TestMethod  m;
        private Throwable   exception;
        private long        start, end;

        TestResult( TestMethod m ) {
            this.m = m;
            this.start = System.currentTimeMillis();
        }

        void done() {
            this.end = System.currentTimeMillis();
        }

        public boolean passed() {
            return exception == null;
        }

        public Throwable getException() {
            return exception;
        }

        public void setException( Throwable exception ) {
            this.exception = exception;
        }

        public long elapsedMillis() {
            return end-start;
        }
    }

}
