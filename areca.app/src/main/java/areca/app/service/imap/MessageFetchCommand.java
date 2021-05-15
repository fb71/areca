/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.app.service.imap;

import static java.lang.String.format;
import static org.apache.james.mime4j.stream.EntityState.T_END_OF_STREAM;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;



//import org.teavm.apachecommons.io.Charsets;
//import org.teavm.apachecommons.io.IOUtils;

import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.MimeConfig;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.apache.james.mime4j.util.CharsetUtil;

import areca.app.service.imap.ImapRequest.Command;

/**
 *
 * @author Falko Bräutigam
 */
public class MessageFetchCommand extends Command {

    public StringBuilder    text = new StringBuilder( 4096 );

    public int              number;

    public String           textContent;

    public String           htmlContent;


    public MessageFetchCommand( int number, String part ) {
        this.number = number;
        command = format( "%s FETCH %d (BODY[%s])", tag, number, part );
        expected = format( "%s OK FETCH completed", tag );
    }


    @Override
    protected void parse( BufferedReader in ) throws Exception {
        in.readLine();
        super.parse( in );

        MimeTokenStream s = new MimeTokenStream( MimeConfig.DEFAULT ) {
            public Reader getReader() throws UnsupportedEncodingException {
                final BodyDescriptor bodyDescriptor = getBodyDescriptor();
                final String mimeCharset = bodyDescriptor.getCharset();
                Charset charset;
                if (mimeCharset == null || "".equals(mimeCharset)) {
                    charset = StandardCharsets.UTF_8;
                } else {
                    charset = CharsetUtil.lookup(mimeCharset);
                    if (charset == null) {
                        charset = StandardCharsets.UTF_8;
                        //throw new UnsupportedEncodingException(mimeCharset);
                    }
                }
                final InputStream instream = getDecodedInputStream();
                return new InputStreamReader(instream, charset);
            }
        };

        s.parse( new StringBuilderInputStream( text ) );

        for (var state = s.getState(); state != T_END_OF_STREAM; state = s.next()) {
            // log.info( "State: " + state );
            switch (state) {
                case T_BODY : {
                    //log.info( "Body: contents = ..." + ", header data = " + s.getBodyDescriptor() );
                    switch (s.getBodyDescriptor().getMimeType()) {
                        case "text/plain" :
                            textContent = toString( s.getReader() );
                            break;
                        case "text/html" :
                            htmlContent = toString( s.getReader() );
                            break;
                    }
                    break;
                }
                default: {
                    //log.info( ":: " + s.getField() );
                    break;
                }
            }
        }
    }


    @Override
    protected boolean parseLine( String line ) {
        if (super.parseLine( line )) {
            text.append( line ).append( "\n" );
            return true;
        }
        return false;
    }


    public static String toString( Reader input ) throws IOException {
        int n = 0;
        char[] buffer = new char[4096];
        StringBuilder result = new StringBuilder( buffer.length);
        while (-1 != (n = input.read( buffer ))) {
            result.append( String.valueOf( buffer, 0, n ) );
        }
        return result.toString();
    }


    public class StringBuilderInputStream extends InputStream {

        protected StringBuilder buffer;
        protected int pos;
        protected int count;

        public StringBuilderInputStream( StringBuilder s ) {
            this.buffer = s;
            count = s.length();
        }

        public int read() {
            return (pos < count) ? (buffer.charAt(pos++) & 0xFF) : -1;
        }

        public int read(byte b[], int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }
            else if ((off < 0) || (off > b.length) || (len < 0) ||
                       ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }
            if (pos >= count) {
                return -1;
            }
            int avail = count - pos;
            if (len > avail) {
                len = avail;
            }
            if (len <= 0) {
                return 0;
            }
            for (int i = 0; i < len; i++) {
                b[i+off] = (byte)read();
            }
            return len;
        }

        public synchronized long skip(long n) {
            if (n < 0) {
                return 0;
            }
            if (n > count - pos) {
                n = count - pos;
            }
            pos += n;
            return n;
        }

        public synchronized int available() {
            return count - pos;
        }

        public synchronized void reset() {
            pos = 0;
        }
    }

}