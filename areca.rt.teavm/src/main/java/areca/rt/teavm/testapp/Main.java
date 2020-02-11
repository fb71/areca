package areca.rt.teavm.testapp;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.reflect.ReflectMethod;

import areca.common.reflect.MethodInfo;
import areca.common.test.AnnotationTest;
import areca.common.testrunner.LogDecorator;
import areca.common.testrunner.Test;
import areca.common.testrunner.TestRunner;
import areca.rt.teavm.reflect.TvmReflectionSupport;
import areca.rt.teavm.ui.TeaApp;
import areca.ui.Button;
import areca.ui.SelectionEvent;
import areca.ui.layout.FillLayout;

/**
 * The test app for the integration tests.
 *
 * @author Falko Bräutigam
 */
@CompileTime
public class Main {

    private static final Logger LOG = Logger.getLogger( Main.class.getName() );

    public static Object getFoo(Object obj) {
        return getFooImpl(obj.getClass(), obj);
    }

    public static Object getFoo(Class<?> cl) {
        return getFooImpl(cl, null);
    }

    @Meta
    private static native Object getFooImpl(Class<?> cls, Object obj);

    private static void getFooImpl(ReflectClass<Object> cl, Value<Object> obj) {
        System.out.println( "META! ---- " + cl );
        for (ReflectMethod m : cl.getMethods()) {
            System.out.println( "    " + m.getName() );
            if (m.getName().equals( "test" )) {
                System.out.println( "        :: " + m.getAnnotation( Test.class ).annotationType() );
            }
        }
        Metaprogramming.exit(() -> {
            return Arrays.asList( new Flyweight( 1 ) );
        });

//        ReflectField field = cl.getField("a");
//        if (field != null) {
//            Metaprogramming.exit(() -> field.get(obj));
//        } else {
//            Metaprogramming.exit(() -> null);
//        }
    }

    @Test
    public void test() {}

    /**
     *
     */
    public static void main( String[] args ) throws Exception {
//        System.out.println( getFoo( new Main() ) );
//        System.out.println( getFoo( Flyweight.class ) );

//        for (Class<?> cl : Tests.all()) {
//            System.out.println( getFoo( cl ) );
//        }

        TvmReflectionSupport.init();
        for (MethodInfo m : TvmReflectionSupport.instance().methodsOf( AnnotationTest.class ).values()) {
            System.out.println( m.name() );
        }

        for (Method m : AnnotationTest.class.getMethods()) {
            LOG.info( "-Method: " + m.getName() + " -> @: " + m.getAnnotations() );
        }
        for (Field f : AnnotationTest.class.getDeclaredFields()) {
            LOG.info( "-Field: " + f.getName() + " -> @: " + f.getDeclaredAnnotations() );
        }

        //ReflectionSupplier

        new TestRunner()
                .addTests( areca.common.test.Tests.all() )
                .addDecorators( LogDecorator.class )
                .run();

  //      CLOG.info( "Commons logging..." );

        //EntityRepository repo = EntityRepository.newConfiguration().create();

        //assert false : "Sehe ich das hier?";

        try {
            TeaApp.instance().createUI( self -> {
                self.layoutManager.set( new FillLayout() );
                // Button1
                self.create( Button.class, btn -> {
                    btn.label.set( "Button! --- !" );
                    btn.layoutConstraints.get().clear();
                    btn.subscribe( (SelectionEvent ev) -> {
                        LOG.info( "clicked: " + ev ); // ev.getType() + ", ctrl=" + ev.getCtrlKey() + ", pos=" + ev.getClientX() + "/" + ev.getClientY() );
                    });
                });
            })
//            // Button2
//            .create( Button.class, self -> {
//                self.label.set( "Button2" );
//                self.bgColor.set( Color.WHITE );
//                self.subscribe( (SelectionEvent ev) -> {
//                    LOG.info( "" + ev );
//                });
//            })
            .layout();
        }
        catch (Exception e) {
            System.out.println( e.getMessage() + " (" + e.getClass().getName() + ")" );
            throw e;
        }


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

        private Queue<String> queue = new LinkedList();

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
