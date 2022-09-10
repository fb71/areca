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
package areca.rt.teavm.testapp;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.apache.commons.lang3.StringUtils;

import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.TestRunner;
import areca.common.testrunner.TestRunner.TestMethod;
import areca.common.testrunner.TestRunner.TestResult;
import areca.common.testrunner.TestRunner.TestStatus;
import areca.common.testrunner.TestRunnerDecorator;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class HtmlTestRunnerDecorator
        extends TestRunnerDecorator {

    private static final Logger LOG = Logger.getLogger( HtmlTestRunnerDecorator.class.getName() );

    public static final ClassInfo<HtmlTestRunnerDecorator> info = HtmlTestRunnerDecoratorClassInfo.instance();

    protected static HTMLDocument   doc = Window.current().getDocument();

    public static HTMLElement       rootElm = doc.getBody();

    protected HTMLElement           testElm;

    protected Map<TestMethod,HTMLElement> testMethodElms = new HashMap<>();

    protected int                   lineWidth;

    protected static int            runCount; // un poco sucio :)


    @Override
    public void preRun( TestRunner runner ) {
        rootElm.getStyle().setProperty( "font", "12px monospace" );

        if (runCount++ > 0) {
            var hr = (HTMLElement)rootElm.appendChild( doc.createElement( "hr" ) );
            hr.getStyle().setProperty( "border-style", "dotted none none none" );
            hr.getStyle().setProperty( "margin-top", "8px" );
            hr.getStyle().setProperty( "width", "60%" );
        }

        rootElm.appendChild( doc.createTextNode( "Running tests..." ) );

        lineWidth = (rootElm.getClientWidth() / 9) - 2;
    }


    @Override
    public void preTest( ClassInfo<?> test ) {
        rootElm.appendChild( testElm = doc.createElement( "p" ));
        testElm.getStyle().setProperty( "margin", "8px 0px 0px 0px" );
        testElm.getStyle().setProperty( "font-weight", "bold" );
        testElm.appendChild( doc.createTextNode( test.name() ) );
    }


    @Override
    public void preTestMethod( TestMethod m ) {
        HTMLElement testMethodElm = doc.createElement( "p" );
        testMethodElms.put( m, testMethodElm );
        rootElm.appendChild( testMethodElm );
        testMethodElm.getStyle().setProperty( "margin", "0px 0px" );
        String l = StringUtils.rightPad( m.name() + " ", lineWidth, '.' );
        testMethodElm.appendChild( doc.createTextNode( l ) );
    }


    @Override
    public void postTestMethod( TestMethod m, TestResult testResult ) {
        HTMLElement span = doc.createElement( "span" );
        span.getStyle().setProperty( "font-weight", "bold" );

        if (testResult.getStatus() == TestStatus.PASSED) {
            span.getStyle().setProperty( "color", "#32bf32" );
            span.appendChild( doc.createTextNode( " ok" ) );
        }
        else if (testResult.getStatus() == TestStatus.SKIPPED) {
            span.getStyle().setProperty( "color", "#fdc84e" );
            span.appendChild( doc.createTextNode( " skip" ) );
        }
        else {
            span.getStyle().setProperty( "color", "#f73737" );
            span.appendChild( doc.createTextNode( " failed" ) );
        }
        var testMethodElm = testMethodElms.get( m );
        testMethodElm.appendChild( span );
        if (testResult.getStatus() == TestStatus.PASSED) {
            testMethodElm.appendChild( doc.createTextNode( String.format( " (%s%s)",
                    testResult.elapsedTime(), testResult.promised ? "~" : "" ) ) );
        }
    }

}
