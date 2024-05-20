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
import java.util.List;

import java.lang.reflect.InvocationTargetException;

import areca.common.AssertionException;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;

/**
 * Handles {@link Promise} test results properly without waiting.
 *
 * @author <a href="http://polymap.de">Falko Bräutigam</a>
 */
public class AsyncAwareTestRunner
        extends TestRunner {

    private static final Log LOG = LogFactory.getLog( AsyncAwareTestRunner.class );

    private static final Object[] NOARGS = new Object[] {};

    // instrance ******************************************

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

    protected void setupTest( ClassInfo<?> cl, Object test ) {
        try {
            Sequence.of( Exception.class, cl.methods() )
                    .filter( m -> m.annotation( BeforeAnnotationInfo.INFO ).isPresent() )
                    .forEach( before -> before.invoke( test, NOARGS ) );
        }
        catch (Exception ee) {
            throw (RuntimeException)ee;
        }
    }


    protected void teardownTest( ClassInfo<?> cl, Object test ) {
        try {
            Sequence.of( Exception.class, cl.methods() )
                    .filter( m -> m.annotation( AfterAnnotationInfo.INFO ).isPresent() )
                    .forEach( after -> after.invoke( test, NOARGS ) );
        }
        catch (Exception ee) {
            throw (RuntimeException)ee;
        }
    }


    @Override
    public AsyncAwareTestRunner run() {
        decorators.forEach( d -> d.preRun( this ) );

        // all test classes
        for (ClassInfo<?> cl : testTypes) {
            decorators.forEach( d -> d.preTest( cl ) );

            // all test methods
            for (TestMethod m : findTestMethods( cl )) {
                TestResult testResult = new TestResult( m );
                started.add( testResult );

                decorators.forEach( d -> d.preTestMethod( m ) );

                Object test = instantiate( cl );
                try {
                    setupTest( cl, test );

                    testResult.start();
                    if (m.m.annotation( Skip.info ).isPresent()) {
                        testResult.skipped = true;
                        testResult.done();
                        decorators.forEach( d -> d.postTestMethod( testResult.m, testResult ) );
                    }
                    else {
                        var result = m.m.invoke( test, NOARGS );

                        if (result instanceof Promise) {
                            testResult.promised = true;
                            ((Promise<?>)result)
                                    .onSuccess( (promise, returnValue) -> {
                                        if (promise.isComplete()) {
                                            onSuccess( testResult );
                                            teardownTest( cl, test );
                                        }
                                    })
                                    .onError( e -> {
                                        onError( testResult, e );
                                        teardownTest( cl, test );
                                    });
                        }
                        else {
                            onSuccess( testResult );
                            teardownTest( cl, test );
                        }
                    }
                }
                catch (Exception e) {
                    onError( testResult, e );
                    teardownTest( cl, test );
                }
                finally {
                    if (!testResult.promised) {
                        decoratorsAfter();
                    }
                }
            }
        }
        return this;
    }


    protected void onSuccess( TestResult testResult ) {
        testResult.done();

        Class<? extends Throwable> expected = testResult.m.m.annotation( Test.info ).get().expected();
        if (!expected.equals( Test.NoException.class )) {
            testResult.setException( new AssertionException( expected, null, "Exception expected" ) );
        }

        decorators.forEach( d -> d.postTestMethod( testResult.m, testResult ) );
    }


    protected void onError( TestResult testResult, Throwable e ) {
        testResult.done();

        var cause = e instanceof InvocationTargetException ? e.getCause() : e;
        Class<? extends Throwable> expected = testResult.m.m.annotation( Test.info ).get().expected();
        if (expected.equals( Test.NoException.class ) || !expected.isAssignableFrom( cause.getClass() )) {
            testResult.setException( cause );
        }

        decorators.forEach( d -> d.postTestMethod( testResult.m, testResult ) );

        // throw (RuntimeException)Platform.rootCause( e );
    }

}
