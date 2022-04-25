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
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.mutable.MutableObject;

import areca.common.AssertionException;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Sequence;
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

    public enum TestStatus {
        PASSED, SKIPPED, FAILED
    }

    // instrance ******************************************

    protected List<ClassInfo<?>>            testTypes = new ArrayList<>( 128 );

    protected List<ClassInfo<? extends TestRunnerDecorator>> decoratorTypes = new ArrayList<>( 128 );

    private List<TestResult>                testResults = new ArrayList<>( 128 );


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
        var decorators = Sequence.of( decoratorTypes ).map( cl -> instantiate( cl ) ).toList();

        // all test classes
        decorators.forEach( d -> d.preRun( this ) );
        for (ClassInfo<?> cl : testTypes) {

            // Before/After
            List<MethodInfo> befores = Sequence.of( cl.methods() )
                    .filter( m -> m.annotation( BeforeAnnotationInfo.INFO ).isPresent() )
                    .toList();
            List<MethodInfo> afters = Sequence.of( cl.methods() )
                    .filter( m -> m.annotation( AfterAnnotationInfo.INFO ).isPresent() )
                    .toList();

            // all test methods
            decorators.forEach( d -> d.preTest( cl ) );
            for (TestMethod m : findTestMethods( cl )) {
                decorators.forEach( d -> d.preTestMethod( m ) );

                // method
                TestResult testResult = new TestResult( m );
                testResults.add( testResult );
                Class<? extends Throwable> expected = m.m.annotation( Test.info ).get().expected();
                try {
                    Object test = instantiate( cl );
                    for (MethodInfo before : befores) {
                        before.invoke( test, NOARGS );
                    }
                    testResult.start();
                    if (m.m.annotation( Skip.info ).isAbsent()) {
                        var result = m.m.invoke( test, NOARGS );

                        if (result instanceof Promise) {
                            var ee = new MutableObject<Throwable>();
                            ((Promise<?>)result)
                                    .onError( e -> ee.setValue( e ) )
                                    .waitForResult();
                            if (ee.getValue() != null) {
                                throw new InvocationTargetException( ee.getValue() );
                            }
                        }
                    }
                    else {
                        testResult.skipped = true;
                    }
                    if (!expected.equals( Test.NoException.class )) {
                        testResult.setException( new AssertionException( expected, null, "Exception expected" ) );
                    }
                    for (MethodInfo after : afters) {
                        after.invoke( test, NOARGS );
                    }
                }
                // error inside test
                catch (InvocationTargetException e ) {
                    if (expected.equals( Test.NoException.class )
                            || !expected.isAssignableFrom( e.getTargetException().getClass() )) {
                        testResult.setException( e.getTargetException() );
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
        return Sequence.of( cl.methods() )
                .filter( m -> m.annotation( TestAnnotationInfo.INFO ).isPresent())
                .map( m -> new TestMethod( m ) )
                .toList();
    }


    /**
     *
     */
    public static class TestMethod {
        protected MethodInfo      m;
        protected AnnotationInfo  a;

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

        public boolean          promised;
        public boolean          skipped;
        public TestMethod       m;

        private Throwable       exception;
        private Timer           timer = Timer.start();
        private String          elapsed;

        TestResult( TestMethod m ) {
            this.m = m;
        }

        TestResult start() {
            this.timer = Timer.start();
            return this;
        }

        void done() {
            elapsed = timer.elapsedHumanReadable();
        }

        public TestStatus getStatus() {
            if (skipped) {
                return TestStatus.SKIPPED;
            }
            else if (exception != null) {
                return TestStatus.FAILED;
            }
            else {
                return TestStatus.PASSED;
            }
        }

        public boolean skipped() {
            return skipped;
        }

        public Throwable getException() {
            return exception;
        }

        public void setException( Throwable exception ) {
            this.exception = exception;
        }

        public String elapsedTime() {
            return elapsed;
        }
    }

}
