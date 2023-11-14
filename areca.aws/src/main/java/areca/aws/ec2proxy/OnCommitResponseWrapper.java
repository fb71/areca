/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.aws.ec2proxy;

import java.util.function.Consumer;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.function.FailableRunnable;

/**
 *
 * @author Falko Bräutigam
 */
public class OnCommitResponseWrapper
        extends HttpServletResponseWrapper {

    private Consumer<String> onCommit;

    private HttpServletResponse delegate;

    public OnCommitResponseWrapper( HttpServletResponse response, Consumer<String> onCommit ) {
        super( response );
        this.onCommit = onCommit;
        this.delegate = (HttpServletResponse)getResponse();
    }

    protected <E extends Exception> void checkCommit( String error, FailableRunnable<E> runnable ) throws E {
        boolean before = delegate.isCommitted();
        runnable.run();
        if (delegate.isCommitted() && !before) {
            onCommit.accept( error );
        }
    }

    @Override
    public void sendError( int sc, String msg ) throws IOException {
        checkCommit( msg, () -> delegate.sendError( sc, msg ) );
    }

    @Override
    public void sendError( int sc ) throws IOException {
        checkCommit( "", () -> delegate.sendError( sc ) );
    }

    @Override
    public void sendRedirect( String location ) throws IOException {
        checkCommit( null, () -> delegate.sendRedirect( location ) );
    }

    @Override
    public void flushBuffer() throws IOException {
        checkCommit( null, () -> delegate.flushBuffer() );
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            private ServletOutputStream out = delegate.getOutputStream();
            @Override
            public void write( int b ) throws IOException {
                out.write( b );
            }
            @Override
            public void setWriteListener( WriteListener writeListener ) {
                out.setWriteListener( writeListener );
            }
            @Override
            public boolean isReady() {
                return isReady();
            }
            @Override
            public void flush() throws IOException {
                checkCommit( null, () -> out.flush() );
            }
            @Override
            public void close() throws IOException {
                checkCommit( null, () -> out.close() );
            }
        };
    }

}
