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
package areca.demo;

import org.teavm.jso.dom.html.HTMLElement;

import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.AsyncAwareTestRunner;
import areca.common.testrunner.LogDecorator;
import areca.rt.teavm.testapp.HtmlTestRunnerDecorator;
import areca.ui.Size;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.layout.RowLayout;
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
    protected UIComponent onCreateUI( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( "Tests" );

        ui.body.layout.set( new FillLayout() );
        ui.body.add( new Text() {{
            content.set( "Waiting...  " );
        }});
        Platform.schedule( 1000, () -> runTests() );
        return ui;
    }


    protected void runTests() {
        try {
            ui.body.components.disposeAll();
            ui.body.add( new ScrollableComposite() {{
                ui.body.layout.set( RowLayout.filled().margins( Size.of( 15, 15 ) ) );
                HtmlTestRunnerDecorator.rootElm = (HTMLElement)htmlElm;
            }} );
            doRunTests();
        }
        catch (Exception e) {
            LOG.debug( "Exception: %s -->   ", e );
            Throwable rootCause = Platform.rootCause( e );
            LOG.debug( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
            throw (RuntimeException)rootCause;
        }
    }


    @SuppressWarnings("unchecked")
    public static void doRunTests() {
        new AsyncAwareTestRunner()
                .addTests(
                        areca.common.test.AnnotationTest.info,
                        areca.common.test.SequenceTest.info,
                        areca.common.test.SequenceOpTest.info,
                        areca.common.test.SameStackEventManagerTest.info,
                        areca.common.test.AsyncEventManagerTest.info,
                        areca.ui.test.UIEventManagerTest.info,
                        areca.common.test.IdleAsyncEventManagerTest.info,
                        // areca.common.test.ThreadedEventManagerTest.info,
                        areca.common.test.RuntimeTest.info,
                        areca.common.test.AsyncTests.info,
                        areca.common.test.SchedulerTest.info
                        )
                .addTests(
                        areca.rt.teavm.test.TeavmRuntimeTest.info )
                .addTests(
                        org.polymap.model2.test2.SimpleModelTest.info,
                        org.polymap.model2.test2.SimpleQueryTest.info,
                        org.polymap.model2.test2.AssociationsTest.info,
                        org.polymap.model2.test2.ComplexModelTest.info,
                        org.polymap.model2.test2.RuntimeTest.info
                        )

                //.addTests( areca.app.service.imap.ImapTest.info )
                //.addTests( areca.app.service.carddav.CardDavTest.info )
                .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                .run();
    }
}
