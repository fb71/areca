package areca.testapp;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import areca.ui.App;
import areca.ui.Button;
import areca.ui.SelectionEvent;
import areca.ui.layout.FillLayout;

public class Main {

    private static final Logger LOG = Logger.getLogger( Main.class.getSimpleName() );

    /**
     *
     */
    public static void main( String[] args ) throws Exception {
//        HTMLDocument doc = HTMLDocument.current();
//
//        HTMLElement div = doc.createElement( "div" );
//        div.getStyle().setProperty( "position", "absolute" );
//        div.getStyle().setProperty( "left", "100px" );
//        div.getStyle().setProperty( "transition", "left 1s ease-in-out, background-color 1s ease-in-out" );
//        div.appendChild( doc.createTextNode( "Teatest2: TeaVM generated element!" ) );
//
//        div.appendChild( doc.createElement( "button", (HTMLElement btn) -> {
//            ((HTMLButtonElement)btn).setNodeValue( "Button" );
//            btn.appendChild(doc.createTextNode("...") );
//            btn.addEventListener( "click", (MouseEvent ev) -> {
//                LOG.info( "clicked: " + ev.getType() + ", ctrl=" + ev.getCtrlKey() + ", pos=" + ev.getClientX() + "/" + ev.getClientY() );
//                div.getStyle().setProperty( "left", "200px" );
////                btn.getStyle().setProperty( "transition", "background-color 1s ease-in-out" );
////                btn.getStyle().setProperty( "background-color", "#808080" );
//            });
//        }));
//
//        doc.getBody().appendChild( div );

        //assert false : "Sehe ich das hier?";

        try {
            App.instance().createUI( self -> {
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

}
