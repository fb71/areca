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
package areca.aws;

import java.util.logging.Logger;

/**
 *
 * @author Falko Bräutigam
 */
public class XLogger {

    public static XLogger get( Class cl ) {
        return new XLogger( Logger.getLogger( cl.getName() ) );
    }

    // instance *******************************************

    private Logger delegate;

    protected XLogger( Logger logger ) {
        this.delegate = logger;
    }

    public Logger delegate() {
        return delegate;
    }

    public XLogger debug( String format, Object... args ) {
        delegate.fine( String.format( format, args ) );
        return this;
    }

    public XLogger info( String format, Object... args ) {
        delegate.info( String.format( format, args ) );
        return this;
    }

    public XLogger warn( String format, Object... args ) {
        delegate.warning( String.format( format, args ) );
        return this;
    }

}
