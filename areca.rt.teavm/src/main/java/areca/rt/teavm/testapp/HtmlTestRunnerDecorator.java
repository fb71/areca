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
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.TestRunner;
import areca.common.testrunner.TestRunner.TestMethod;
import areca.common.testrunner.TestRunner.TestResult;
import areca.common.testrunner.TestRunnerDecorator;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class HtmlTestRunnerDecorator
        extends TestRunnerDecorator {

    private static final Logger LOG = Logger.getLogger( HtmlTestRunnerDecorator.class.getName() );

    protected HTMLDocument          doc;

    protected HTMLElement           testElm;

    protected HTMLElement           testMethodElm;


    @Override
    public void preRun( TestRunner runner ) {
        doc = Window.current().getDocument();
        doc.getBody().getStyle().setProperty( "font", "12px monospace" );
        doc.getBody().appendChild( doc.createTextNode( "Starting..." ) );
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
        String l = (m.name() + " ............................................................").substring( 0, 60 );
        testMethodElm.appendChild( doc.createTextNode( l ) );
    }


    @Override
    public void postTestMethod( TestMethod m, TestResult testResult ) {
        HTMLElement span = doc.createElement( "span" );
        span.getStyle().setProperty( "font-weight", "bold" );
        span.getStyle().setProperty( "color", testResult.passed() ? "green" : "red" );
        testMethodElm.appendChild( span );
        span.appendChild( doc.createTextNode( testResult.passed() ? " ok" : " failed" ) );
        testMethodElm.appendChild( doc.createTextNode( " (" + testResult.elapsedMillis() + "ms)" ) );
    }

}
