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
package areca.aws.logs;

import areca.aws.logs.EventCollector.Event;
import areca.aws.logs.EventCollector.EventTransform;

/**
 *
 * @author Falko Bräutigam
 */
public class NullTransformer<P>
        extends EventTransform<P,P> {

    @Override
    public P apply( Event<P> ev ) {
        return ev.data;
    }

}
