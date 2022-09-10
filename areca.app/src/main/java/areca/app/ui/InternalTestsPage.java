/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app.ui;

import org.teavm.jso.dom.html.HTMLElement;

import org.polymap.model2.test2.SimpleQueryTest;

import areca.app.BenchTest;
import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.test.SequenceOpTest;
import areca.common.testrunner.AsyncAwareTestRunner;
import areca.common.testrunner.LogDecorator;
import areca.common.testrunner.TestRunner;
import areca.rt.teavm.testapp.HtmlTestRunnerDecorator;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;

/**
 *
 * @author Falko Bräutigam
 */
public class InternalTestsPage
        extends Page {

    private static final Log LOG = LogFactory.getLog( InternalTestsPage.class );

    private PageContainer ui;

    @Override
    protected UIComponent doInit( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Tests" );

        ui.body.layout.set( new FillLayout() );
        Platform.schedule( 1000, () -> runTests() );
        return ui;
    }


    @SuppressWarnings("unchecked")
    protected void runTests() {
        try {
            HtmlTestRunnerDecorator.rootElm = (HTMLElement)ui.body.htmlElm;
            new TestRunner()
                    .addTests(
                            BenchTest.info,
                            SequenceOpTest.info
                    )
                    .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                    .run();

            new AsyncAwareTestRunner()
                    .addTests( SimpleQueryTest.info )
                    .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                    .run();
        }
        catch (Exception e) {
            LOG.debug( "Exception: %s -->   ", e );
            Throwable rootCause = Platform.rootCause( e );
            LOG.debug( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
            throw (RuntimeException)rootCause;
        }
    }

}
