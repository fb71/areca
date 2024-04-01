/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLScriptElement;
import org.teavm.jso.dom.xml.Document;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * API of CodeMirror JavaScript code.
 *
 * @author Falko Bräutigam
 */
public abstract class CodeMirror
        implements JSObject {

    private static final Log LOG = LogFactory.getLog( CodeMirror.class );

    @JSBody( params = {"parent", "options"}, script = "return CodeMirror( parent, options );" )
    public static native CodeMirror create( HTMLElement parent, CodeMirror.Options options );

    @JSBody( params = {"fn", "options"}, script = "return CodeMirror( fn, options );" )
    public static native CodeMirror create( CodeMirror.Options options, Callback fn );

    @JSBody( params = {"textarea", "options"}, script = "return CodeMirror.fromTextArea( textarea, options );" )
    public static native CodeMirror fromTextArea( HTMLElement textarea, CodeMirror.Options options );

    @JSMethod
    public abstract String getValue();

    @JSMethod
    public abstract void setValue( String value );

    @JSMethod
    public abstract void on( String type, Callback handler );

    @JSMethod
    public abstract void setSize( int width, int height );

    @JSMethod
    public abstract void refresh();

    @JSMethod
    public abstract Cursor getCursor();

    @JSMethod
    public abstract String getLine( int num );

    /**
     *
     */
    public static abstract class Cursor
            implements JSObject {

        @JSProperty("ch")
        public abstract int ch();

        @JSProperty("line")
        public abstract int line();
    }

    /**
     *
     */
    public static abstract class Options
            implements JSObject {

        @JSBody( script = "return {};" )
        public static native CodeMirror.Options create();

        @JSProperty
        public abstract void setLineWrapping( boolean value );

        @JSProperty
        public abstract void setTheme( String theme );

        @JSProperty
        public abstract void setMode( String mode );

        @JSProperty
        public abstract void setMode( JSObject mode );

        @JSProperty
        public abstract void setLineNumbers( boolean value );

        @JSBody( params = {}, script = "this.extraKeys = {\"Ctrl-Space\": \"autocomplete\"};" )
        public abstract void enableAutocompleteKey();
        //            @JSBody( params = {"keyCode", "value"}, script = "this.extraKeys = {'keyCode': 'value'};" )
        //            public abstract void setExtraKeys( String keyCode, String value );

        //hintOptions: {hint: synonyms}
        @JSProperty
        public abstract void setHintOptions( CodeMirror.HintOptions options );
    }

    /**
     *
     */
    public static abstract class HintOptions
            implements JSObject {

        @JSBody( script = "return {};" )
        public static native CodeMirror.HintOptions create();

        @JSProperty
        public abstract void setHint( Callback2 fn );
    }

    /**
     *
     */
    public static abstract class HintResult
            implements JSObject {

        @JSBody( script = "return {};" )
        public static native CodeMirror.HintResult create();

        @JSBody( params = {"line", "ch"}, script = "return {\"line\":line, \"ch\":ch};" )
        public static native JSObject createPos( int line, int ch );

        @JSProperty
        public abstract void setList( JSArray value );

        @JSProperty
        public abstract void setFrom( JSObject value );

        @JSProperty
        public abstract void setTo( JSObject value );
    }


    @JSFunctor
    public interface Callback extends JSObject {
        void handle( JSObject _1 );
    }


    @JSFunctor
    public interface Callback2 extends JSObject {
        JSObject handle( JSObject _1, JSObject _2 );
    }


    /**
     *
     * @see <a href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/">CloudFlare</a>
     */
    public static Promise<Void> injectJsCss( Document doc ) {
        // https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/
        injectJsCss( doc, "codemirror/codemirror.min.css" );
        injectJsCss( doc, "codemirror/theme/mbo.min.css" );
        injectJsCss( doc, "codemirror/show-hint.min.css" );

        var t = Timer.start();
        return injectJsCss( doc, "codemirror/codemirror.min.js" )
                .then( __ -> {
                    LOG.info( "codemirror.js loaded: %s", t ); t.restart();
                    injectJsCss( doc, "codemirror/markdown.min.js" );
                    injectJsCss( doc, "codemirror/css.min.js" );
                    injectJsCss( doc, "codemirror/show-hint.min.js" );
                    return injectJsCss( doc, "codemirror/css-hint.min.js" );
                })
                .then( __ -> {
                    LOG.info( "modules loaded: %s", t );
                    // wait another 100ms ...
                    return Platform.schedule( 100, () -> null );
                });
    }


    public static Promise<Void> injectJsCss( Document doc, String filename ) {
        if (filename.endsWith( ".js" )) {
            var scriptElm = (HTMLScriptElement2)doc.createElement( "script" );
            scriptElm.setAttribute( "type", "text/javascript" );
            scriptElm.setAttribute( "src", filename );
            var p = new Promise.Completable<Void>();
            scriptElm.onLoad( __ -> p.complete( null ) );
            doc.getElementsByTagName( "head" ).get( 0 ).appendChild( scriptElm );
            return p;
        }
        else if (filename.endsWith( ".css" )) {
            var elm = doc.createElement( "link" );
            elm.setAttribute( "rel", "stylesheet" );
            elm.setAttribute( "type", "text/css" );
            elm.setAttribute( "href", filename );
            doc.getElementsByTagName( "head" ).get( 0 ).appendChild( elm );
            return null;
        }
        else {
            throw new RuntimeException( "Unknown file type: " + filename );
        }
    }

    /**
     *
     */
    protected static abstract class HTMLScriptElement2
            implements HTMLScriptElement {

        @JSProperty("onload")
        public abstract void onLoad( Callback fn );
    }

}