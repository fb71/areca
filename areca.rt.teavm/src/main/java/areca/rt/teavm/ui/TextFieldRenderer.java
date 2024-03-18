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

import java.util.List;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLScriptElement;
import org.teavm.jso.dom.html.HTMLTextAreaElement;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.Timer;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.NoRuntimeInfo;
import areca.common.reflect.RuntimeInfo;
import areca.rt.teavm.ui.TextFieldRenderer.CodeMirror.HintOptions;
import areca.rt.teavm.ui.TextFieldRenderer.CodeMirror.HintResult;
import areca.ui.component2.Events.EventType;
import areca.ui.component2.TextField;
import areca.ui.component2.TextField.Type;
import areca.ui.component2.UIComponentEvent;
import areca.ui.component2.UIComponentEvent.ComponentConstructedEvent;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class TextFieldRenderer
        extends RendererBase {

    private static final Log LOG = LogFactory.getLog( TextFieldRenderer.class );

    public static final ClassInfo<TextFieldRenderer> TYPE = TextFieldRendererClassInfo.instance();

    private static volatile boolean jsCssInjected = false;

    @NoRuntimeInfo
    public static void _start() {
        UIComponentEvent.manager()
                .subscribe( new TextFieldRenderer() )
                .performIf( ev -> ev instanceof ComponentConstructedEvent && ev.getSource() instanceof TextField );
    }

    // instance *******************************************

    @EventHandler( ComponentConstructedEvent.class )
    public void componentConstructed( ComponentConstructedEvent ev ) {
        TextField c = (TextField)ev.getSource();
        if (c.multiline.get()) {
            createCodeMirror( c );
        }
        else {
            createField( c );
        }
    }

    /**
     * Form single-line text field.
     */
    @NoRuntimeInfo
    protected void createField( TextField c ) {
        var input = (HTMLInputElement)(c.htmlElm = doc().createElement( "input" ));
        if (c.type.$() == Type.USERNAME) {
            input.setAttribute( "type", "text" );
            input.setAttribute( "id", "username" );
            input.setAttribute( "name", "username" );
            input.setAttribute( "autocomplete", "username" );
        }
        else if (c.type.$() == Type.PASSWORD) {
            input.setAttribute( "type", "password" );
            input.setAttribute( "id", "password" );
            input.setAttribute( "name", "password" );
            input.setAttribute( "autocomplete", "current-password" );
        }
        else {
            input.setAttribute( "type", "text" );
        }

        c.content.onInitAndChange( (newValue, oldValue) -> {
            LOG.debug( "Set: %s", newValue );
            input.setValue( newValue != null ? newValue : "" );
        });

        LOG.debug( "Register listener: %s", "input" );
        input.addEventListener( "input", htmlEv -> {
            htmlEv.stopPropagation();
            htmlEv.preventDefault();

            c.content.rawSet( input.getValue() );
            LOG.debug( "Text: %s", input.getValue() );
            propagateEvent( c, htmlEv, EventType.TEXT );
        });
    }

    /**
     * <textarea>
     */
    @NoRuntimeInfo
    protected void createTextArea( TextField c ) {
        var textarea = (HTMLTextAreaElement)(c.htmlElm = doc().createElement( "textarea" ));

        c.content.onInitAndChange( (newValue, oldValue) -> {
            LOG.debug( "Set: %s", newValue );
            textarea.setValue( newValue != null ? newValue : "" );
        });
        textarea.addEventListener( "input", htmlEv -> {
            htmlEv.stopPropagation();
            htmlEv.preventDefault();

            c.content.rawSet( textarea.getValue() );
            LOG.debug( "Text: %s", textarea.getValue() );
            propagateEvent( c, htmlEv, EventType.TEXT );
        });
    }

    /**
     * CodeMirror
     */
    @NoRuntimeInfo
    protected void createCodeMirror( TextField c ) {
        var container = (HTMLElement)(doc().createElement( "div" ));
        container.getStyle().setProperty( "overflow", "hidden" );
        c.htmlElm = container;

        checkJsCss().onSuccess( __ -> {
            // options
            var options = CodeMirror.Options.create();
            options.setLineNumbers( false );
            options.setLineWrapping( true );
            options.setTheme( "mbo" );
            switch (c.type.get()) {
                case MARKDOWN: options.setMode( "markdown" ); break;
                case CSS: options.setMode( "css" ); break;
                default: options.setMode( (String)null ); break;
            }
            options.enableAutocompleteKey();

            // hintOptions
            var hintOptions = HintOptions.create();
            hintOptions.setHint( (cm,_hintOptions) -> {
                LOG.info( "hint() called ");
                var result = HintResult.create();
                c.<List<String>>optData( "_autocomplete_" ).ifPresent( l -> {
                    LOG.info( "hint(): %s", l );
                    var a = JSArray.create();
                    l.forEach( s -> a.push( JSString.valueOf( s ) ) );
                    result.setList( a );
                    result.setFrom( HintResult.createPos( 0, 0 ) );
                    result.setTo( HintResult.createPos( 0, 0 ) );
                });
                return result;
            });
            options.setHintOptions( hintOptions );

            // CodeMirror
            var cm = CodeMirror.create( container, options );

            // properties
            c.content.onInitAndChange( (newValue, oldValue) -> {
                cm.setValue( newValue != null ? newValue : "" );
            });
            c.autocomplete.onInitAndChange( (newValues,___) -> {
                LOG.debug( "autocompletions: %s", newValues );
                c.setData( "_autocomplete_", newValues );
            });
            c.size.onInitAndChange( (newValue,___) -> {
                // wait for (Page open) transform to finish
                Platform.schedule( 1500, () -> {
                    LOG.debug( "refresh" );
                    cm.refresh();
                });
            });

            // editor content changed
            cm.on( "change", htmlEv -> {
                //LOG.info( "changed (%s)", cm.getValue() );
                c.content.rawSet( cm.getValue() );
                propagateEvent( c, null, EventType.TEXT );
            });
        });
    }

    /**
     *
     */
    public static abstract class CodeMirror
            implements JSObject {

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


        /**
         */
        protected static abstract class Options
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
            public abstract void setHintOptions( HintOptions options );
        }

        /**
         */
        protected static abstract class HintOptions
                implements JSObject {

            @JSBody( script = "return {};" )
            public static native CodeMirror.HintOptions create();

            @JSProperty
            public abstract void setHint( Callback2 fn );
        }

        /**
         */
        protected static abstract class HintResult
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
    }

    @JSFunctor
    public interface Callback extends JSObject {
        void handle( JSObject _1 );
    }

    @JSFunctor
    public interface Callback2 extends JSObject {
        JSObject handle( JSObject _1, JSObject _2 );
    }

    @NoRuntimeInfo
    private Promise<Void> checkJsCss() {
        if (!jsCssInjected) {
            jsCssInjected = true;
            injectJsCss( "codemirror/codemirror.min.css" );
            injectJsCss( "codemirror/theme/mbo.min.css" );
            injectJsCss( "codemirror/show-hint.min.css" );

            var t = Timer.start();
            return injectJsCss( "codemirror/codemirror.min.js" )
                    .then( __ -> {
                        LOG.debug( "codemirror.js loaded: %s", t );
                        injectJsCss( "codemirror/markdown.min.js" );
                        injectJsCss( "codemirror/css.min.js" );
                        injectJsCss( "codemirror/show-hint.min.js" );
                        return injectJsCss( "codemirror/css-hint.min.js" );
                    })
                    .onSuccess( __ -> {
                        LOG.debug( "modules loaded: %s", t );
                    });
        }
        else {
            return Promise.completed( null, Priority.BACKGROUND );
        }
    }

    @NoRuntimeInfo
    private Promise<Void> injectJsCss( String filename ) {
        if (filename.endsWith( ".js" )) {
            var scriptElm = (HTMLScriptElement2)doc().createElement( "script" );
            scriptElm.setAttribute( "type", "text/javascript" );
            scriptElm.setAttribute( "src", filename );
            var p = new Promise.Completable<Void>();
            scriptElm.onLoad( __ -> p.complete( null ) );
            doc().getElementsByTagName( "head" ).get( 0 ).appendChild( scriptElm );
            return p;
        }
        else if (filename.endsWith( ".css" )) {
            var elm = doc().createElement( "link" );
            elm.setAttribute( "rel", "stylesheet" );
            elm.setAttribute( "type", "text/css" );
            elm.setAttribute( "href", filename );
            doc().getElementsByTagName( "head" ).get( 0 ).appendChild( elm );
            return null;
        }
        else {
            throw new RuntimeException( "Unknown file type: " + filename );
        }
    }

    /**
     */
    protected static abstract class HTMLScriptElement2
            implements HTMLScriptElement {

        @JSProperty("onload")
        public abstract void onLoad( Callback fn );
    }


}
