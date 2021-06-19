package areca.app;

import areca.common.Platform;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.testrunner.LogDecorator;
import areca.common.testrunner.TestRunner;
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
//            new TestRunner()
//                    .addTests( areca.common.test.SequenceOpTest.info )
//                    .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
//                    .run();

            new TestRunner()
                    //.addTests( org.polymap.model2.test2.AssociationsModelTest.info )

                    //.addTests( areca.common.test.Tests.all() )
                    //.addTests( org.polymap.model2.test2.Tests.all() )
                    //.addTests( TeavmRuntimeTest.info )
                    //.addTests( SetTimeoutEventManagerTest.info )
                    //.addTests( areca.app.service.imap.ImapTest.info )
                    .addTests( areca.app.service.carddav.CardDavTest.info )
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
