/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app.service.matrix;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;

import areca.common.Promise;
import areca.common.Promise.Completable;

/**
 *
 */
public interface JSPromise<T extends JSObject>
        extends JSObject {

    @JSMethod
    public JSPromise<T> then( Callback1<T> callback );

    @JSMethod("catch")
    public JSPromise<T> catch_( Callback1<JSCommon> err );

    public default Promise<T> asPromise() {
        var result = new Completable<T>();
        then( (T value) -> result.complete( value ) );
        catch_( err -> {
            MatrixClient.console( err );
            result.completeWithError( new RuntimeException( "Error in JSPromise. See above for error." ) );
        });
        return result;
    }
}