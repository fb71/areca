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

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

import org.teavm.jso.dom.xml.Document;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.Node;
import org.teavm.jso.dom.xml.NodeList;

import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Convenience and helpers for {@link Document} handling.
 *
 * @author Falko Bräutigam
 */
public class SimpleDocument {

    private static final Log log = LogFactory.getLog( SimpleDocument.class );

    public static SimpleDocument parseXml( String xml ) {
        DOMParser parser = DOMParser.create();
        return new SimpleDocument( parser.parseFromString( xml, "text/xml" ) );
    }

    // instance *******************************************

    protected Document      doc;

    public SimpleDocument( Document doc ) {
        this.doc = doc;
    }

    public Document doc() {
        return doc;
    }


    public boolean isParseError() {
        return !getElementsByTagName( "parseerror" ).isEmpty();
    }


    public List<Element> getElementsByTagName( String name ) {
        return new NodeListWrapper<>( doc.getElementsByTagName( name ) );
    }

    public Sequence<Element,RuntimeException> elementsByTagName( String name ) {
        return Sequence.of( getElementsByTagName( name ) );
    }


//    public <E extends Exception> void ifElementWithTagNamePresent( String name, Consumer<Element,E> consumer ) throws E {
//        List<Element> elms = getElementsByTagName( name );
//        if (elms
//    }

    public List<Element> getElementsByTagNameNS( String namespaceURI, String localName ) {
        return new NodeListWrapper<>( doc.getElementsByTagNameNS( namespaceURI, localName ) );
    }


    /**
     *
     */
    protected static class NodeListWrapper<T extends Node>
            extends AbstractList<T>
            implements RandomAccess {

      private final NodeList list;

        NodeListWrapper( NodeList l ) {
            list = l;
        }

        @SuppressWarnings("unchecked")
        public T get( int index ) {
            return (T)list.item( index );
        }

        public int size() {
            return list.getLength();
        }
    }
}
