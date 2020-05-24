/*
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package areca.systemservice.client;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.dom.xml.Document;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.NodeList;

import areca.common.ProgressMonitor;
import areca.common.Timer;
import areca.common.base.Consumer;
import areca.common.base.Function;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class SystemServiceClient {

    private static final Log log = LogFactory.getLog( SystemServiceClient.class );

    public static SystemServiceClient connect( String baseUri ) {
        return new SystemServiceClient( baseUri );
    }


    // instance *******************************************

    private String      baseUri;


    protected SystemServiceClient( String baseUri ) {
        this.baseUri = baseUri;
    }


    public void close() {
    }


    public void process( Path path, HierarchyVisitor visitor, ProgressMonitor monitor ) {
        new HierarchyWalker( this, visitor, monitor ).process( path );
    }


    public <R,E extends Exception> ResponseFuture<R,E> fetchFolder( Path path,
            Function<List<FolderEntry>,R,E> onSuccess,
            Consumer<E,RuntimeException> onError ) {
        String uri = baseUri + "/" + path;
        log.info( "fetchFolder(): " + uri );
        Timer timer = Timer.start();
        XMLHttpRequest request = createRequest();
        request.open( "PROPFIND", uri, true ); //, "user", "password" );
        request.setRequestHeader( "Depth", "1" );

        ResponseFuture<R,E> result = new ResponseFuture<>();
        request.onComplete( () -> {
            try {
                // response status
                log.info( "Status: " + request.getStatus() );
                if (request.getStatus() > 299) {
                    throw new IOException( "HTTP Status: " + request.getStatus() );
                }
                // parse XML
                Document doc = request.getResponseXML();
                NodeList<Element> hrefs = doc.getElementsByTagName( "d:href" );
                log.info( "fetchFolder(): " + hrefs.getLength() + " (" + timer.elapsedHumanReadable() + ")" );

                // create FolderEntry
                ArrayList<FolderEntry> entries = new ArrayList<>();
                for (int i=1; i<hrefs.getLength(); i++) {
                    entries.add( new FolderEntry( parsePath( hrefs.get( i ).getFirstChild().getNodeValue() ) ) );
                }
                result.setValue( onSuccess.apply( entries ) );
            }
            catch (Exception e) {
                onError.accept( (E)e );
            }
        });
        timer.restart();
        request.send();
        return result;
    }

    /**
     * Represents the result of an asynchronous {@link SystemServiceClient} method.
     *
     * @param <T>
     * @param <E>
     */
    public static class ResponseFuture<T, E extends Exception> {

        protected volatile T       value;

        protected volatile boolean cancelled;

        protected volatile boolean done;

        public boolean cancel( boolean mayInterruptIfRunning ) {
            cancelled = true;
            return true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public boolean isDone() {
            return done;
        }

        public T waitAndGet() throws E {
            if (!done) {
                synchronized (this) {
                    while (!done) {
                        try {
                            wait();
                        }
                        catch (InterruptedException e) {
                        }
                    }
                }
            }
            return value;
        }

        protected void setValue( T newValue ) {
            synchronized (this) {
                this.done = true;
                this.value = newValue;
                notifyAll();
            }
        }
    }


    protected Path parsePath( String pathString ) {
        // XXX strip contextPath
        return Path.parse( pathString ).stripFirst( 2 );
    }


    protected XMLHttpRequest createRequest() {
        XMLHttpRequest request = XMLHttpRequest.create();
        request.setOnReadyStateChange( () -> {
            log.info( "ready state: " + request.getReadyState() );
        });
        return request;
    }

//  public void doRequest() {
//  Timer timer = Timer.start();
//  XMLHttpRequest request = XMLHttpRequest.create();
//  request.setOnReadyStateChange( () -> {
//      log.info( "ready state: " + request.getReadyState() );
//  });
//  request.onComplete( () -> {
//      log.info( "complete: " + timer.elapsedHumanReadable() );
//      log.info( "complete: type=" + request.getResponseType() );
//      log.info( "complete: status=" + request.getStatus() );
//      log.info( "complete: url=" + request.getResponseURL() );
//      log.info( "complete: headers=" + request.getAllResponseHeaders() );
//      log.info( "complete: content=" + request.getResponseText() );
//
//      Document doc = request.getResponseXML();
//      NodeList<Element> responses = doc.getElementsByTagName( "headers" ); //d:href" );
//      for (int i=0; i<responses.getLength(); i++) {
//          log.info( "response: " + responses.get( i ).getFirstChild().getNodeValue() );
//      }
//      log.info( "Body: " + doc.getElementsByTagName( "plainBody" ).get( 0 ).getFirstChild().getNodeValue() );
//  });
//  request.open( "GET", "webdav/support@polymap.de/Sent/messages/chunk-1/message-1/envelope.xml", true );
//  //request.open( "PROPFIND", "webdav/support@polymap.de", true ); //, "user", "password" );
//  //request.setRequestHeader( "Accept", "application/json" );
//  request.setRequestHeader( "Depth", "1" );
//  request.send();
//  timer.restart();
//}

}
