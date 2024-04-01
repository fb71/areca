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

import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLTextAreaElement;

import areca.common.Platform;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.event.EventHandler;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.NoRuntimeInfo;
import areca.common.reflect.RuntimeInfo;
import areca.rt.teavm.ui.CodeMirror.HintOptions;
import areca.rt.teavm.ui.CodeMirror.HintResult;
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

        checkInjectJsCss().onSuccess( __ -> {
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

            // hintOptions
            var hintOptions = HintOptions.create();
            hintOptions.setHint( (cm,_hintOptions) -> computeHints( (CodeMirror)cm, c ) );
            options.setHintOptions( hintOptions );
            options.enableAutocompleteKey();

            // CodeMirror
            var cm = CodeMirror.create( container, options );

            // properties
            c.content.onInitAndChange( (newValue, oldValue) -> {
                cm.setValue( newValue != null ? newValue : "" );
            });
            c.autocomplete.onInitAndChange( (newValues,___) -> {
                LOG.debug( "completions: %s", newValues );
                c.setData( "_completions_", newValues );
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
     * Compute autocomplete hints - from the list of possible completions and the
     * current cursor position.
     */
    protected HintResult computeHints( CodeMirror cm, TextField tf ) {
        LOG.debug( "computeHints(): ...");
        var result = HintResult.create();
        tf.<List<String>>optData( "_completions_" ).ifPresent( completions -> {
            LOG.debug( "computeHints(): %s", completions );

            var l = JSArray.create();
            var cursor = cm.getCursor();
            var line = cm.getLine( cursor.line() );
            LOG.debug( "computeHints(): %s : %s", line, cursor.ch() );

            var start = cursor.ch();
            while (start > 0 && isCompletable( line.charAt( start-1 ) )) {
                start --;
            }
            var word = line.substring( start, cursor.ch() );
            LOG.debug( "computeHints(): start = %s -> '%s'", start, word );

            for (var c : completions) {
                if (c.startsWith( word )) {
                    l.push( JSString.valueOf( c ) );
                }
            }

            result.setList( l );
            result.setFrom( HintResult.createPos( cursor.line(), start ) );
            result.setTo( HintResult.createPos( cursor.line(), cursor.ch() ) );
        });
        return result;
    }

    protected boolean isCompletable( char c ) {
        return Character.isLetter( c ) || c == '/';
    }

    protected Promise<Void> checkInjectJsCss() {
        if (!jsCssInjected) {
            jsCssInjected = true;
            return CodeMirror.injectJsCss( doc() );
        }
        else {
            return Promise.completed( null, Priority.BACKGROUND );
        }
    }

}
