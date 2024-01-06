package areca.rt.server.test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Running tests from Areca core modules.
 *
 * @author Falko Bräutigam
 */
public class ArecaCoreTests extends JUnitTestBase {

    private static final Log LOG = LogFactory.getLog( ArecaCoreTests.class );

    @Test
    public void sequenceTest() {
        execute( areca.common.test.SequenceTest.info );
    }

    @Test
    public void sequenceOpTest() {
        execute( areca.common.test.SequenceOpTest.info );
    }

    @Test
    public void xhrTest() throws InterruptedException {
        execute( areca.common.test.XhrTest.info );
    }

    @Test
    public void asyncTest() {
        execute( areca.common.test.AsyncTests.info );
    }

    @Test
    public void runtimeTest() {
        execute( areca.common.test.RuntimeTest.info );
    }

    @Test
    @Disabled
    public void schedulerTest() {
        execute( areca.common.test.SchedulerTest.info );
    }

    @Test
    public void asyncEventManagerTest() {
        execute( areca.common.test.AsyncEventManagerTest.info );
    }

    @Test
    public void idleAsyncEventManagerTest() {
        execute( areca.common.test.IdleAsyncEventManagerTest.info );
    }

    @Test
    public void sameStackEventManagerTest() {
        execute( areca.common.test.SameStackEventManagerTest.info );
    }

    @Test
    public void annotationTest() {
        execute( areca.common.test.AnnotationTest.info );
    }

    @Test
    public void uiEventManagerTest() {
        execute( areca.ui.test.UIEventManagerTest.info );
    }

}
