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

    protected HTMLDocument          doc;

    protected HTMLElement           testElm;

    protected HTMLElement           testMethodElm;

    protected int                   lineWidth;


    @Override
    public void preRun( TestRunner runner ) {
        doc = Window.current().getDocument();
        doc.getBody().getStyle().setProperty( "font", "12px monospace" );
        doc.getBody().appendChild( doc.createTextNode( "Running tests..." ) );

        lineWidth = (doc.getBody().getClientWidth() / 9) - 2;
    }


    @Override
    public void preTest( ClassInfo<?> test ) {
        doc.getBody().appendChild( testElm = doc.createElement( "p" ));
        testElm.getStyle().setProperty( "margin", "8px 0px 0px 0px" );
        testElm.getStyle().setProperty( "font-weight", "bold" );
        testElm.appendChild( doc.createTextNode( test.name() ) );
    }


    @Override
    public void preTestMethod( TestMethod m ) {
        doc.getBody().appendChild( testMethodElm = doc.createElement( "p" ));
        testMethodElm.getStyle().setProperty( "margin", "0px 0px" );
        String l = StringUtils.rightPad( m.name() + " ", lineWidth, '.' );
        testMethodElm.appendChild( doc.createTextNode( l ) );
    }


    @Override
    public void postTestMethod( TestMethod m, TestResult testResult ) {
        HTMLElement span = doc.createElement( "span" );
        span.getStyle().setProperty( "font-weight", "bold" );

        if (testResult.getStatus() == TestStatus.PASSED) {
            span.getStyle().setProperty( "color", "green" );
            span.appendChild( doc.createTextNode( " ok" ) );
        }
        else if (testResult.getStatus() == TestStatus.SKIPPED) {
            span.getStyle().setProperty( "color", "orange" );
            span.appendChild( doc.createTextNode( " skipped" ) );
        }
        else {
            span.getStyle().setProperty( "color", "red" );
            span.appendChild( doc.createTextNode( " failed" ) );
        }
        testMethodElm.appendChild( span );
        testMethodElm.appendChild( doc.createTextNode( " (" + testResult.elapsedMillis() + "ms)" ) );
    }

}
