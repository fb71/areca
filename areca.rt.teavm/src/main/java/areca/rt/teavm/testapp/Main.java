package areca.rt.teavm.testapp;

import java.util.logging.Logger;

import areca.rt.teavm.ui.TeaApp;
import areca.ui.component.Button;
import areca.ui.component.SelectionEvent;
import areca.ui.layout.FillLayout;

/**
 * The test app for the integration tests.
 *
 * @author Falko Bräutigam
 */
public class Main {

    private static final Logger LOG = Logger.getLogger( Main.class.getName() );

    /**
     *
     */
    public static void main( String[] args ) throws Exception {
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
    }

}
