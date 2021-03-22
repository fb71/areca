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
package areca.app.service.email;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.teavm.jso.dom.xml.Element;

import org.apache.commons.lang.StringUtils;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;

import areca.app.model.Message;
import areca.common.ProgressMonitor;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.systemservice.client.HierarchyVisitor;
import areca.systemservice.client.Path;
import areca.systemservice.client.SimpleDocument;
import areca.ui.component.Property;

/**
 *
 * @author Falko Bräutigam
 */
public class EmailFolderSynchronizer
        extends HierarchyVisitor {

    private static final Log log = LogFactory.getLog( EmailFolderSynchronizer.class );

    public Property<Boolean>        checkExisting = Property.create( this, "checkExisting", false );

    protected volatile int          fileCount;

    public Opt<Exception>           exception = Opt.absent();

    public Map<Path,String>         envelopes;


    @Override
    public boolean acceptsFolder( Path path ) {
        return true;
    }


    @Override
    public boolean acceptsFile( Path path ) {
        return path.lastPart().equals( "envelope.xml" );
    }


    @Override
    public void visitFile( Path path, Object content ) {
        if (envelopes == null) {
            envelopes = new HashMap<>( 256 );
        }
        envelopes.put( path, (String)content );
    }


    @Override
    public void onError( Exception e ) {
        exception = Opt.of( e );
        e.printStackTrace();
    }


    public void processEnvelopes( EntityRepository repo, ProgressMonitor monitor ) {
        for (Entry<Path,String> envelope : envelopes.entrySet()) {
            if (monitor.isCancelled()) {
                return;
            }
            fileCount ++;
            try (
                UnitOfWork uow = repo.newUnitOfWork();
            ){
                uow.createEntity( Message.class, null, (Message proto) -> {
                    SimpleDocument doc = SimpleDocument.parseXml( envelope.getValue() );
                    for (Element elm : doc.getElementsByTagName( "headers" )) {
                        String[] kv = StringUtils.splitByWholeSeparator( elm.getFirstChild().getNodeValue(), "::" );
                        //log.info( "KeyValue: " + Arrays.asList( kv ) );
                        if (kv[0].equalsIgnoreCase( "from" )) {
                            proto.from.set( kv[1] );
                        }
                    }
                    doc.elementsByTagName( "htmlBody" ).first().ifPresent( elm -> {
                        proto.text.set( elm.getFirstChild().getNodeValue() );
                    });
                    doc.elementsByTagName( "plainBody" ).first().ifPresent( elm -> {
                        proto.text.set( elm.getFirstChild().getNodeValue() );
                    });
                    return proto;
                });
                uow.commit();
            }
        }
    }

}
