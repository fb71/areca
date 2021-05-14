package areca.app;

import areca.common.Platform;
import areca.common.testrunner.AsyncTestRunner;
import areca.common.testrunner.LogDecorator;
import areca.rt.teavm.testapp.HtmlTestRunnerDecorator;

/**
 * The test app for the integration tests.
 *
 * @author Falko Bräutigam
 */
public class TestsMain {

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public static void main( String[] args ) throws Exception {
        try {
            new AsyncTestRunner()
                    .addTests( areca.common.test.AsyncTests.info )
                    .addTests( org.polymap.model2.test2.Tests.all() )
                    .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                    .run();
//            new TestRunner()
//                    .addTests( areca.common.test.AsyncTests.info )
//                    .addTests( areca.common.test.Tests.all() )
//                    .addTests( org.polymap.model2.test2.Tests.all() )
//                    .addTests( TeavmRuntimeTest.info )
//                    .addTests( SetTimeoutEventManagerTest.info )
//                    .addTests( EmailServiceTest.info )
//                    .addTests( areca.systemservice.client.test.Tests.all() )
//                    .addTests( ImapTest.info )
//                    .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
//                    .run();
        }
        catch (Exception e) {
            System.out.println( "Exception: " + e + " --> " );
            Throwable rootCause = Platform.instance().rootCause( e );
            System.out.println( "Root cause: " + rootCause + " : " + rootCause.getMessage() );
            throw (Exception)rootCause;
        }
    }

}
