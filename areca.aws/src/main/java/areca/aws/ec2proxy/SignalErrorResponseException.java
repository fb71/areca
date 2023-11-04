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

/**
 * Signals that the request could no be processed because of a resource is not found
 * or any other reason, which is not an exception but causes the servlet to responde
 * with the given status code and message (instead of 200).
 *
 * @author Falko Bräutigam
 */
public class SignalErrorResponseException
        extends RuntimeException {

    public final int status;

    public SignalErrorResponseException( int status, String message, Throwable cause ) {
        super( message, cause );
        this.status = status;
    }

    public SignalErrorResponseException( int status, String message ) {
        super( message );
        this.status = status;
    }
}
