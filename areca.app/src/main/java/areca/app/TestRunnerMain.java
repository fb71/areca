package areca.app;

import areca.app.service.email.EmailServiceTest;
import areca.common.testrunner.LogDecorator;
import areca.common.testrunner.TestRunner;
import areca.rt.teavm.test.TeavmRuntimeTest;
import areca.rt.teavm.testapp.HtmlTestRunnerDecorator;

/**
 * The test app for the integration tests.
 *
 * @author Falko Bräutigam
 */
public class TestRunnerMain {

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public static void main( String[] args ) throws Exception {
        try {
            new TestRunner()
                    .addTests( areca.common.test.Tests.all() )
                    .addTests( org.polymap.model2.test2.Tests.all() )
                    .addTests( TeavmRuntimeTest.info )
                    .addTests( EmailServiceTest.info )
                    .addTests( areca.systemservice.client.test.Tests.all() )
                    .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                    .run();
        }
        catch (Exception e) {
            System.out.println( "Exception: " + e + " --> " );
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                Throwable parent = rootCause.getCause();
                if (parent == rootCause) {
                    break;
                }
                rootCause = parent;
            }
            System.out.println( "Root cause: " + rootCause + " : " + rootCause.getMessage() );
            throw (Exception)rootCause;
        }
    }

}
