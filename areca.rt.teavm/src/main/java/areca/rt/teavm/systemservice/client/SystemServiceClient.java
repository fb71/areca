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
package areca.rt.teavm.systemservice.client;

import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.dom.xml.Document;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.NodeList;

import areca.common.Timer;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class SystemServiceClient {

    private static final Log log = LogFactory.getLog( SystemServiceClient.class );


    public void doRequest() {
        Timer timer = Timer.start();
        XMLHttpRequest request = XMLHttpRequest.create();
        request.setOnReadyStateChange( () -> {
            log.info( "ready state: " + request.getReadyState() );
        });
        request.onComplete( () -> {
            log.info( "complete: " + timer.elapsedHumanReadable() );
            log.info( "complete: type=" + request.getResponseType() );
            log.info( "complete: status=" + request.getStatus() );
            log.info( "complete: url=" + request.getResponseURL() );
            log.info( "complete: headers=" + request.getAllResponseHeaders() );
            log.info( "complete: content=" + request.getResponseText() );
            log.info( "complete: content=" + request.getResponseXML().getNodeName() );

            Document doc = request.getResponseXML();
            NodeList<Element> responses = doc.getElementsByTagName( "d:href" );
            log.info( "getLength(): " + responses.getLength() );
            for (int i=0; i<responses.getLength(); i++) {
                log.info( "response: " + responses.get( i ).getFirstChild().getNodeValue() );
            }
        });
        request.open( "PROPFIND", "webdav/support@polymap.de", true ); //, "user", "password" );
        //request.setRequestHeader( "Accept", "application/json" );
        request.setRequestHeader( "Depth", "1" );
        request.send();
        timer.restart();
    }

}
