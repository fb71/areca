/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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
package areca.ui.component2;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.Property.ReadWrite;

/**
 *
 * @author Falko Bräutigam
 */
public class FileUpload
        extends UIComponent {

    private static final Log LOG = LogFactory.getLog( FileUpload.class );

    public ReadWrite<FileUpload,File> data = Property.rw( this, "data" );

    @Override
    public int computeMinHeight( int width ) {
        return 32;
    }


    /**
     * A file/data uploaded via {@link FileUpload}.
     */
    public interface File
            extends Blob {

        /**
         * Returns the last modified time of the file, in millisecond since the UNIX
         * epoch (January 1st, 1970 at Midnight).
         */
        public int lastModified();

        /**
         * Returns the name of the file referenced by the File object.
         */
        public String name();
    }


    /**
     * A file/data uploaded via {@link FileUpload}.
     */
    public interface Blob {

        /** JS File object on the client side. */
        public Object underlying();

        public byte[] data();

        /**
         * The size, in bytes, of the data contained in the Blob object.
         */
        public int size();

        /**
         * A string indicating the MIME type of the data contained in the Blob. If
         * the type is unknown, this string is empty.
         */
        public String mimetype();
    }

}
