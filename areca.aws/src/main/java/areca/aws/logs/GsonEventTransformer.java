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
 * Non-pretty json for {@link OpenSearchSink} + simple event logging.
 *
 * @author Falko Bräutigam
 */
public class GsonEventTransformer<P>
        extends EventTransform<P,String> {

    @Override
    public String apply( Event<P> ev ) {
//        if (ev.data instanceof HttpRequestEvent) {
//            var data = (HttpRequestEvent)ev.data;
//            OpenSearchSink.LOG.debug( "%s: %s %s - %s (%s)", data.vhost, data.method, data.url, data.status, data.error );
//        }
//        else if (ev.data instanceof Ec2InstanceEvent) {
//            var data = (Ec2InstanceEvent)ev.data;
//            OpenSearchSink.LOG.debug( "%s: %s -> %s", data.vhost, data.isRunningBefore, data.isRunningAfter );
//        }
//        else {
//            OpenSearchSink.LOG.debug( "No log for: %s", ev.data.getClass().getSimpleName() );
//        }
        return OpenSearchSink.gson.toJson( ev.data );
    }

}
