package areca.app;

import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.AsyncAwareTestRunner;
import areca.common.testrunner.LogDecorator;
import areca.rt.teavm.testapp.HtmlTestRunnerDecorator;

/**
 * The test app for the integration tests.
 *
 * @author Falko Bräutigam
 */
public class TestsMain {

    private static final Log LOG = LogFactory.getLog( TestsMain.class );

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public static void main( String[] args ) throws Exception {
        try {
            new AsyncAwareTestRunner()
                    .addTests(
                            areca.common.test.AnnotationTest.info,
                            areca.common.test.SequenceTest.info,
                            areca.common.test.SequenceOpTest.info,
                            areca.common.test.SameStackEventManagerTest.info,
                            areca.common.test.AsyncEventManagerTest.info,
                            areca.ui.test.UIEventManagerTest.info,
                            areca.common.test.IdleAsyncEventManagerTest.info,
//                            areca.common.test.ThreadedEventManagerTest.info,
                            areca.common.test.RuntimeTest.info,
                            areca.common.test.AsyncTests.info
                    )
                    .addTests(
                            areca.rt.teavm.test.TeavmRuntimeTest.info
                    )
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
        catch (Exception e) {
            LOG.debug( "Exception: %s -->", e );
            Throwable rootCause = Platform.rootCause( e );
            LOG.debug( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
            throw (Exception)rootCause;
        }
    }

}
