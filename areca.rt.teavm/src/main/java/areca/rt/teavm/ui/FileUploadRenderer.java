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
package areca.rt.teavm.ui;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLInputElement;

import areca.common.Platform;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Level;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.Position;
import areca.ui.component2.Events;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.FileUpload;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class FileUploadRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( FileUploadRenderer.class );

    public static final ClassInfo<FileUploadRenderer> TYPE = FileUploadRendererClassInfo.instance();

    public static void _start() {
        UIComponentEvent.manager()
                .subscribe( new FileUploadRenderer() )
                .performIf( ComponentConstructedEvent.class, ev -> ev.getSource() instanceof FileUpload );
    }

    static {
        LogFactory.setClassLevel( FileUploadRenderer.class, Level.DEBUG );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        var c = (FileUpload)ev.getSource();

        var input = (HTMLFileInputElement)(c.htmlElm = doc().createElement( "input" ));
        input.setAttribute( "type", "file" );

        input.addEventListener( "change", htmlEv -> {
            htmlEv.stopPropagation();
            htmlEv.preventDefault();

            var f = input.files().get( 0 );
            //HTMLFileInputElement.console( f );
            LOG.debug( "File name: %s", f.name() );

            c.data.rawSet( new FileUpload.File() {
                @Override public Object underlying() { return f; }
                @Override public int size() { return f.size(); }
                @Override public String mimetype() { return f.mimetype(); }
                @Override public int lastModified() { return f.lastModified(); }
                @Override public String name() { return f.name(); }
                @Override
                public byte[] data() {
                    throw new RuntimeException( "not yet implemented (on client side)" );
                }
            });
            propagateEvent( c, htmlEv );
        });
    }


    /** */
    public static abstract class HTMLFileInputElement
            implements HTMLInputElement {

        @JSBody(params = {"obj"}, script = "console.log( obj );")
        public static native void console( JSObject obj );

        @JSProperty("files")
        public abstract JSArray<JSFile> files();

    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/API/File
     * <p>
     * Unfortunately can not implement {@link File} directly.
     */
    public static abstract class JSFile
            implements JSObject {

        @JSProperty("lastModified")
        public abstract int lastModified();

        @JSProperty("size")
        public abstract int size();

        @JSProperty("name")
        public abstract String name();

        @JSProperty("type")
        public abstract String mimetype();
    }


    protected void propagateEvent( FileUpload c, Event htmlEv ) {
        LOG.debug( "propagateEvent(): ...");
        for (var handler : c.events) {
            if (handler.type == EventType.UPLOAD) {
                try {
                    var uiev = new Events.UIEvent( c, htmlEv, handler.type ) {
                        @Override public Position clientPos() {
                            return null;
                        }
                    };
                    handler.consumer.accept( uiev );
                }
                catch (Exception e) {
                    Throwable rootCause = Platform.rootCause( e );
                    LOG.info( "Root cause: %s : %s", rootCause, rootCause.getMessage() );
                    throw (RuntimeException)e; // help TeaVM to print proper stack
                }
            }
        }
    }

}
