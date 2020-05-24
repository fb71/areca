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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import areca.common.Timer;
import areca.common.base.Sequence;
import areca.systemservice.FolderResourceBase;


/**
 *
 * @author Falko Bräutigam
 */
public class EmailFolderResource
        extends FolderResourceBase {

    private static final Log log = LogFactory.getLog( EmailFolderResource.class );

    public static final int     CHUNK_SIZE = 10;

    private Folder folder;


    public EmailFolderResource( Folder f ) {
        this.folder = f;
    }

    @Override
    public String getName() {
        return folder.getName();
    }

    @Override
    protected Iterable<? extends FolderResourceBase> createChildren() throws Exception {
        return Arrays.asList( new FoldersFolderResource(), new MessagesFolderResource() );
    }


    /**
     *
     */
    public class FoldersFolderResource
            extends FolderResourceBase {

        @Override
        public String getName() {
            return "folders";
        }

        @Override
        protected Iterable<EmailFolderResource> createChildren() throws Exception {
            return Sequence.of( parent( AccountFolderResource.class ).childrenOf( folder ) )
                    .transform( EmailFolderResource::new )
                    .asIterable();
        }
    }


    /**
     *
     */
    public class MessagesFolderResource
            extends FolderResourceBase {

       @Override
       public String getName() {
           return "messages";
       }

       @Override
       protected Iterable<MessagesChunkFolderResource> createChildren() throws Exception {
           List<MessagesChunkFolderResource> result = new ArrayList<>( 128 );
           int messageCount = folder.getMessageCount();
           log.debug( "messages: " + messageCount );
           if (messageCount > 0) {
               for (int i=0; i < (messageCount/CHUNK_SIZE)+1; i++) {
                   int start = i * CHUNK_SIZE;
                   int size = Math.min( CHUNK_SIZE, messageCount - start );
                   result.add( new MessagesChunkFolderResource( start, size ) );
               }
           }
           return result;
       }
   }


    /**
     *
     */
    public class MessagesChunkFolderResource
            extends FolderResourceBase {

        private int startIndex;

        private int chunkSize;

        public MessagesChunkFolderResource( int startIndex, int chunkSize ) {
            this.startIndex = startIndex;
            this.chunkSize = chunkSize;
        }

        @Override
        public String getName() {
            return "chunk-" + (startIndex+1);
        }

        @Override
        protected Iterable<MessageFolderResource> createChildren() throws Exception {
            try {
                if (!folder.isOpen()) {
                    log.debug( "Opening folder: " + folder.getName() );
                    folder.open( Folder.READ_ONLY );
                }
                log.debug( "Get messages: " + folder.getName() + ": start=" + startIndex );
                Message[] messages = folder.getMessages( startIndex+1, startIndex+chunkSize );

                log.debug( "Fetching messages: " + messages.length );
                Timer t = Timer.start();
                FetchProfile profile = new FetchProfile();
                profile.add( FetchProfile.Item.FLAGS );
                profile.add( FetchProfile.Item.ENVELOPE );
                profile.add( FetchProfile.Item.CONTENT_INFO );
                profile.add( "X-mailer" );
                folder.fetch( messages, profile );
                log.debug( "Fetched: " + messages.length + " messages (" + t.elapsedHumanReadable() + ")" );

                return Sequence.of( messages ).transform( MessageFolderResource::new ).asIterable();
            }
            finally {
                folder.close( false );
            }
        }
    }

}
