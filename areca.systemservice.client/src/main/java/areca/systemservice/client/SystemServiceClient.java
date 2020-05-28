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
public class SystemServiceClient
        implements AutoCloseable {

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


    public ResponseFuture<Object,RuntimeException> process( Path path, HierarchyVisitor visitor, ProgressMonitor monitor ) {
        return new HierarchyWalker( this, visitor, monitor ).process( path );
    }


    public <R,E extends Exception> ResponseFuture<R,E> fetchFile( Path path,
            Function<String,R,E> onSuccess,
            Consumer<Exception,RuntimeException> onError ) {
        String uri = baseUri + "/" + path;
        log.info( "fetchFile(): " + uri );
        Timer timer = Timer.start();
        XMLHttpRequest request = createRequest();
        request.open( "GET", uri, true ); //, "user", "password" );

        ResponseFuture<R,E> result = new ResponseFuture<>();
        request.onComplete( () -> {
            try {
                log.info( "Status: " + request.getStatus() );
                if (request.getStatus() > 299) {
                    throw new IOException( "HTTP Status: " + request.getStatus() );
                }
                String content = request.getResponseText();
                result.setValue( onSuccess.apply( content ) );
            }
            catch (Exception e) {
                onError.accept( e );
                result.setException( e );
            }
        });
        timer.restart();
        request.send();
        return result;
    }


    public <R,E extends Exception> ResponseFuture<R,E> fetchFolder( Path path,
            Function<List<FolderEntry>,R,E> onSuccess,
            Consumer<Exception,RuntimeException> onError ) {
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
                onError.accept( e );
                result.setException( e );
            }
        });
        timer.restart();
        request.send();
        return result;
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
