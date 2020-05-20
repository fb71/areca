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

import areca.common.base.Sequence;


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
    protected Iterable<? extends FolderChildBase> createChildren() throws Exception {
        return Arrays.asList( new FoldersFolderResource(), new MessagesFolderResource() );
    }


    /**
     *
     */
    public class FoldersFolderResource
            extends FolderChildBase {

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
            extends FolderChildBase {

       @Override
       public String getName() {
           return "messages";
       }

       @Override
       protected Iterable<MessagesChunkFolderResource> createChildren() throws Exception {
           List<MessagesChunkFolderResource> result = new ArrayList<>( 128 );
           for (int i=0; i < folder.getMessageCount(); i += CHUNK_SIZE) {
               result.add( new MessagesChunkFolderResource( i ) );
           }
           return result;
       }
   }


    /**
     *
     */
    public class MessagesChunkFolderResource
            extends FolderChildBase {

        private int startIndex;

        public MessagesChunkFolderResource( int startIndex ) {
            this.startIndex = startIndex;
        }

        @Override
        public String getName() {
            return "chunk-" + startIndex;
        }

        @Override
        protected Iterable<MessageFolderResource> createChildren() throws Exception {
            Message[] messages = folder.getMessages( startIndex, startIndex+CHUNK_SIZE );

            FetchProfile metadataProfile = new FetchProfile();
            // load flags, such as SEEN (read), ANSWERED, DELETED, ...
            metadataProfile.add( FetchProfile.Item.FLAGS );
            // also load From, To, Cc, Bcc, ReplyTo, Subject and Date
            metadataProfile.add( FetchProfile.Item.ENVELOPE );
            // we could as well load the entire messages (headers and body, including all "attachments")
            // metadataProfile.add(IMAPFolder.FetchProfileItem.MESSAGE);
            folder.fetch( messages, metadataProfile );

            return Sequence.of( messages )
                    .transform( MessageFolderResource::new )
                    .asIterable();
        }
    }

    /**
     *
     */
    public abstract class FolderChildBase
            extends FolderResourceBase {
    }

}
