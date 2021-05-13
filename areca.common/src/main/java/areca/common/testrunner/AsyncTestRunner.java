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
import java.util.HashSet;
import java.util.List;

import java.lang.reflect.InvocationTargetException;

import areca.common.AssertionException;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;

/**
 * Very simple test framework.
 * <p>
 * JUnit is not an option as it might not be supported by all possible runtimes.
 *
 * @author <a href="http://polymap.de">Falko Bräutigam</a>
 */
public class AsyncTestRunner
        extends TestRunner {

    private static final Log LOG = LogFactory.getLog( AsyncTestRunner.class );

    private static final Object[] NOARGS = new Object[] {};

    public enum TestStatus {
        PASSED, SKIPPED, FAILED
    }

    // instrance ******************************************

    private ArrayList<? extends TestRunnerDecorator> decorators;

    private List<TestResult>        completed = new ArrayList<>( 128 );

    private List<TestResult>        started = new ArrayList<>( 128 );


    protected void decoratorsBefore() {
        //decorators.forEach( d -> d.preTest( cl ) );
    }


    protected void decoratorsAfter() {
        if (completed.size() == started.size()) {
            decorators.forEach( d -> d.postRun( this ) );
        }
//        decorators.forEach( d -> d.postTestMethod( m, testResult ) );
//    }
//    decorators.forEach( d -> d.postTest( cl ) );
//}
//decorators.forEach( d -> d.postRun( this ) );

    }


    @Override
    public void run() {
        decorators = Sequence.of( decoratorTypes ).map( cl -> instantiate( cl ) ).toList();
        decorators.forEach( d -> d.preRun( this ) );

        var testsStarted = new HashSet<ClassInfo<?>>();

        // all test classes
        for (ClassInfo<?> cl : testTypes) {
            // Before/After
            var befores = Sequence.of( cl.methods() )
                    .filter( m -> m.annotation( BeforeAnnotationInfo.INFO ).isPresent() )
                    .toList();
            var afters = Sequence.of( cl.methods() )
                    .filter( m -> m.annotation( AfterAnnotationInfo.INFO ).isPresent() )
                    .toList();

            // all test methods
            for (TestMethod m : findTestMethods( cl )) {
                TestResult testResult = new TestResult( m );
                started.add( testResult );

                Platform.instance().async( () -> {
                    if (testsStarted.add( cl )) {
                        decorators.forEach( d -> d.preTest( cl ) );
                    }
                    decorators.forEach( d -> d.preTestMethod( m ) );

                    var promised = false;
                    try {
                        Object test = instantiate( cl );
                        for (var before : befores) {
                            before.invoke( test, NOARGS );
                        }
                        testResult.start();
                        if (m.m.annotation( Skip.info ).isPresent()) {
                            testResult.skipped = true;
                            testResult.done();
                            decorators.forEach( d -> d.postTestMethod( testResult.m, testResult ) );
                        }
                        else {
                            var result = m.m.invoke( test, NOARGS );

                            if (result instanceof Promise) {
                                promised = true;
                                ((Promise<?>)result)
                                        .onSuccess( (promise, returnValue) -> {
                                            if (promise.isComplete()) {
                                                onSuccess( testResult );
                                            }
                                        })
                                        .onError( e -> {
                                            onError( testResult, e );
                                        });
                            }
                            else {
                                onSuccess( testResult );
                            }
                        }
                        for (var after : afters) {
                            after.invoke( test, NOARGS );
                        }
                    }
                    catch (Exception e) {
                        onError( testResult, e );
                    }
                    finally {
                        if (!promised) {
                            decoratorsAfter();
                        }
                    }
                });
            }
        }
    }


    protected void onSuccess( TestResult testResult ) {
        Class<? extends Throwable> expected = testResult.m.m.annotation( Test.info ).get().expected();
        if (!expected.equals( Test.NoException.class )) {
            testResult.setException( new AssertionException( expected, null, "Exception expected" ) );
        }
        testResult.done();
        decorators.forEach( d -> d.postTestMethod( testResult.m, testResult ) );
    }


    protected void onError( TestResult testResult, Throwable e ) {
        if (e instanceof InvocationTargetException) {
            InvocationTargetException ite = (InvocationTargetException)e;
            System.out.println( "getCause()..." + e );
            Class<? extends Throwable> expected = testResult.m.m.annotation( Test.info ).get().expected();
            if (expected.equals( Test.NoException.class )
                    || !expected.isAssignableFrom( ite.getTargetException().getClass() )) {
                testResult.setException( ite.getTargetException() );
            }
        }
        else {
            testResult.setException( e );
        }
        testResult.done();
        decorators.forEach( d -> d.postTestMethod( testResult.m, testResult ) );
    }

}
