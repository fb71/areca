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

import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import areca.common.base.Opt;
import areca.common.base.Consumer.RConsumer;

/**
 * Provides means of handling "undefined" JS results.
 */
public interface JSCommon<T>
        extends JSObject {

    @NoSideEffects
    @JSBody(params = {}, script = "return this === null || typeof this === 'undefined';")
    public abstract boolean isUndefined();

    public default Opt<T> opt() {
        return Opt.of( isUndefined() ? (T)null : this.cast() );
    }

    public default T ifPresent( RConsumer<T> consumer ) {
        if (!isUndefined()) {
            consumer.accept( this.cast() );
        }
        return this.cast();
    }
}