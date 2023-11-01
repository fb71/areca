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

import java.util.concurrent.Callable;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

/**
 *
 * @author Falko Bräutigam
 */
public class Lazy<T>
        extends LazyInitializer<T> {

    public static <R> Lazy<R> of( Callable<R> supplier ) {
        return new Lazy<>( supplier );
    }

    // instance *******************************************

    private Callable<T> supplier;

    public Lazy( Callable<T> supplier ) {
        this.supplier = supplier;
    }

    @Override
    protected T initialize() throws ConcurrentException {
        try {
            return supplier.call();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }

}
