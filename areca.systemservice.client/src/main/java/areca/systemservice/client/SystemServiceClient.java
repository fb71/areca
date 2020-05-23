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

import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.dom.xml.Document;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.NodeList;

import areca.common.ProgressMonitor;
import areca.common.Timer;
import areca.common.base.Consumer;
import areca.common.base.Sequence;
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


    public void process( Path path, WebdavHierarchyVisitor visitor, ProgressMonitor monitor ) {
        if (monitor.isCancelled()) {
            return;
        }
        fetchFolder( path,
                entries -> {
                    if (visitor.visitFolder( path, entries )) {
                        Sequence.of( entries ).forEach( entry -> process( entry.path, visitor, monitor ) );
                    }
                },
                e -> visitor.onError( e ) );
    }


    public <E extends Exception> void fetchFolder( Path path,
            Consumer<List<FolderEntry>,E> onSuccess,
            Consumer<E,RuntimeException> onError ) {
        log.info( "fetchFolder(): " + path );
        Timer timer = Timer.start();
        XMLHttpRequest request = createRequest();
        request.open( "PROPFIND", path.toString(), true ); //, "user", "password" );
        //request.setRequestHeader( "Accept", "application/json" );
        request.setRequestHeader( "Depth", "1" );
        request.onComplete( () -> {
            try {
                log.info( "fetchFolder(): completed in " + timer.elapsedHumanReadable() );
                Document doc = request.getResponseXML();
                NodeList<Element> hrefs = doc.getElementsByTagName( "d:href" );

                ArrayList<FolderEntry> entries = new ArrayList<>();
                for (int i=0; i<hrefs.getLength(); i++) {
                    entries.add( new FolderEntry( path( hrefs.get( i ).getFirstChild().getNodeValue() ) ) );
                }
                onSuccess.accept( entries );
            }
            catch (Exception e) {
                onError.accept( (E)e );
            }
        });
        timer.restart();
        request.send();
    }


    protected Path path( String nodeValue ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
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
