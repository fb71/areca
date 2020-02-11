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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Very simple test framework.
 * <p>
 * JUnit is not an option as it might not be supported by all possible runtimes.
 *
 * @author <a href="http://polymap.de">Falko Bräutigam</a>
 */
public class TestRunner {

    /** */
    public static abstract class Decorator {

        public void preRun( TestRunner runner ) {};

        public void postRun( TestRunner runner ) {};

        public void preTest( Object test ) {};

        public void postTest( Object test ) {};

        public void preTestMethod( TestMethod m ) {};

        public void postTestMethod( TestMethod m ) {};

    }

    /** */
    public static class TestMethod {
        private Method      m;
        private Annotation  a;

        public TestMethod( Method m ) {
            this.m = m;
            this.a = m.getAnnotation( Test.class );
        }

        public String name() {
            return m.getName();
        }
    }


    /** */
    public static class TestResult {

        private TestMethod  m;

        private Throwable   exception;

        private long        start, end;

        public TestResult( TestMethod m ) {
            this.m = m;
            this.start = System.currentTimeMillis();
        }

        public void done() {
            this.end = System.currentTimeMillis();
        }

        public Throwable getException() {
            return exception;
        }

        public void setException( Throwable exception ) {
            this.exception = exception;
        }

    }


    // instrance ******************************************

    private List<Class>                         testClasses = new ArrayList<>( 128 );

    private List<Class<? extends Decorator>>    decoratorClasses = new ArrayList<>( 128 );

    private List<TestResult>                    testResults = new ArrayList<>( 128 );


    public TestRunner addTests( Class... newTests ) {
        testClasses.addAll( Arrays.asList( newTests ) );
        return this;
    }


    public TestRunner addDecorators( Class<? extends Decorator> cls ) {
        decoratorClasses.addAll( Arrays.asList( cls ) );
        return this;
    }


    public void run() {
        List<Decorator> decorators = decoratorClasses.stream().map( cl -> instantiate( cl ) ).collect( Collectors.toList() );

        // all test classes
        decorators.forEach( d -> d.preRun( this ) );
        for (Class cl : testClasses) {
            Object test = instantiate( cl );

            // all test methods
            decorators.forEach( d -> d.preTest( test ) );
            for (TestMethod m : findTestMethods( cl )) {
                decorators.forEach( d -> d.preTestMethod( m ) );

                // method
                TestResult testResult = new TestResult( m );
                testResults.add( testResult );
                try {
                    m.m.invoke( test, new Object[0] );
                }
                catch (Throwable e ) {
                    testResult.setException( e );
                }
                finally {
                    testResult.done();
                }
                decorators.forEach( d -> d.postTestMethod( m ) );
            }
            decorators.forEach( d -> d.postTest( test ) );
        }
        decorators.forEach( d -> d.postRun( this ) );
    }


    @SuppressWarnings("deprecation")
    protected <R> R instantiate( Class<R> cl ) {
        try {
            return cl.newInstance();
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
            System.out.println( e );
            throw new RuntimeException( e );
        }
    }


    protected List<TestMethod> findTestMethods( Class cl ) {
        return Arrays.stream( cl.getMethods() )
                .filter( m -> m.getAnnotation( Test.class ) != null)
                .map( m -> new TestMethod( m ) )
                .collect( Collectors.toList() );
    }

}
