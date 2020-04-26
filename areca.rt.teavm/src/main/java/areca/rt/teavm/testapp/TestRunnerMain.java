package areca.rt.teavm.testapp;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import areca.common.testrunner.LogDecoratorClassInfo;
import areca.common.testrunner.TestRunner;

/**
 * The test app for the integration tests.
 *
 * @author Falko Bräutigam
 */
public class TestRunnerMain {

    private static final Logger LOG = Logger.getLogger( TestRunnerMain.class.getName() );

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public static void main( String[] args ) throws Exception {
        new TestRunner()
                .addTests( areca.common.test.Tests.all() )
                .addDecorators( LogDecoratorClassInfo.INFO, HtmlTestRunnerDecoratorClassInfo.INFO )
                .run();

//        for (Field f : mainWindow.getClass().getDeclaredFields()) {
//            LOG.info( "Field: " + f );
//            f.setAccessible( true );
//            if (String.class.isAssignableFrom(f.getType())) {
//                f.set(mainWindow, "Annotated!" );
//            }
//        }


//        HandlerThread handler = new HandlerThread();
//        handler.start();
//        handler.add( "1" ); //.add( "2" );
//
//        for (int i=0; i<10; i++) {
//            log( "[" + i + "] ..." );
//            for (int c=0; c<1000; c++) {
//                new Flyweight(c);
//            }
//            System.gc();
//            Thread.sleep( 1000 );
//        }
    }


    static class Flyweight {
        int n;

        public Flyweight( int n ) {
            this.n = n;
        }

        @Override
        protected void finalize() throws Throwable {
            log("finalize(): " + n);
        }
    }


    /** */
    static class HandlerThread extends Thread {

        private Queue<String> queue = new LinkedList<>();

        public HandlerThread add( String msg ) {
            synchronized(queue) {
                queue.offer( msg );
                queue.notifyAll();
            }
            return this;
        }

        @Override
        public void run() {
            log("[thread] ...");
            while (true) {
                synchronized( queue ) {
                    try {
                        if (!queue.isEmpty()) {
                            log( "[thread] " + queue.remove() );
                        }
                        else {
                            queue.wait();
                        }
                    }
                    catch (InterruptedException e) {
                        log( "[thread] interrupted!" );
                    }
                }
            }
        }
    }


    protected static void log( String msg ) {
        //System.out.println( "[INFO]: " + msg );
        LOG.info( msg );
    }

    public static void log2( Object... parts ) {
        System.out.println( "log() ..." );
        for (Object part : parts) {
            System.out.print( part.toString() );
        }
        Arrays.stream( parts ).forEach( part -> System.out.print( part.toString() ) );
        System.out.println();
    }

}
