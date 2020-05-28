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
package areca.systemservice.email;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import java.io.IOException;
import java.io.OutputStream;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.util.MimeMessageParser;

import areca.common.base.Sequence;
import areca.systemservice.FolderResourceBase;
import areca.systemservice.ResourceBase;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;

/**
 *
 * @author Falko Bräutigam
 */
public class MessageFolderResource
        extends FolderResourceBase {

    private static final Log log = LogFactory.getLog( MessageFolderResource.class );

    private Message      message;


    public MessageFolderResource( Message message ) {
        this.message = message;
    }

    @Override
    public String getName() {
        return "message-" + message.getMessageNumber();
    }

    @Override
    protected Iterable<? extends Resource> createChildren() throws Exception {
        return Arrays.asList( new EnvelopeResource( message ) );
    }


    /**
     *
     */
    @XmlRootElement(name = "envelope")
    public static class EnvelopeResource
            extends ResourceBase
            implements GetableResource {

        private Message message;

        private MimeMessageParser parser;


        protected EnvelopeResource() {
        }

        public EnvelopeResource( Message message ) {
            this.message = message;
        }

        @Override
        public String getName() {
            return "envelope.xml";
        }

        @Override
        public void sendContent( OutputStream out, Range range, Map<String,String> params, String contentType )
                throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
            try {
                // XXX message.getFolder().open( Folder.READ_ONLY );
                parser = new MimeMessageParser( (MimeMessage)message ).parse();

                JAXBContext jaxbContext = JAXBContext.newInstance( EnvelopeResource.class );
                Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
                marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF8" );
                marshaller.marshal( this, out );
            }
            catch (Exception e) {
                throw new RuntimeException( e );
            }
            finally {
                parser = null;
//                try {
//                    message.getFolder().close( false );
//                }
//                catch (MessagingException e) {
//                    throw new RuntimeException( e );
//                }
            }
        }

        @XmlElement
        public String getMessageNumber() {
            return String.valueOf( message.getMessageNumber() );
        }

        @XmlElement
        public String getSubject() throws MessagingException {
            return message.getSubject();
        }

        @XmlElement
        public String getPlainBody() throws Exception {
            return parser.getPlainContent();
        }

        @XmlElement
        public String getHtmlBody() throws Exception {
            return parser.getHtmlContent();
        }

        @XmlElement
        public String getSentDate() throws MessagingException {
            return String.valueOf( message.getSentDate().getTime() );
        }

        @XmlElement
        public String getReceivedDate() throws MessagingException {
            return String.valueOf( message.getReceivedDate().getTime() );
        }

        @XmlElement
        public Collection<String> getFlags() throws MessagingException {
            return Sequence.of( message.getFlags().getSystemFlags() ).transform( Flag::toString ).asCollection();
        }

//        @XmlElement
//        @SuppressWarnings("unchecked")
//        public Collection<String> getHeaders() throws Exception {
//            return Sequence.of( Collections.<Header>list( message.getAllHeaders() ) )
//                    .transform( h -> h.getName() + ":" + h.getValue() )
//                    .asCollection();
//        }

        @XmlElement
        public Collection<String> getReceipients() throws MessagingException {
            return Sequence.of( message.getAllRecipients() )
                    .transform( Address::toString )
                    .asCollection();
        }

        @Override
        public Long getMaxAgeSeconds( Auth auth ) {
            return null;
        }

        @Override
        public String getContentType( String accepts ) {
            return "text/xml";
        }

        @Override
        public Long getContentLength() {
            return null;
        }
    }

}
