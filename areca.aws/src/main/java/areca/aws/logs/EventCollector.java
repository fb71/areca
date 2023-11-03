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

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;

import java.time.Duration;
import java.time.Instant;

import areca.aws.XLogger;

/**
 *
 * @param <S> The type of the record send to the sink
 * @param <P> The type of the paylod of the {@link Event}
 * @author Falko Bräutigam
 */
public class EventCollector<P,S> {

    private static final XLogger LOG = XLogger.get( EventCollector.class );

    public static final Duration INTERVAL = Duration.ofSeconds( 3 );

    public static final int CHUNK_SIZE_MAX = 100;

    /**
     *
     */
    public static class Event<T> {
        public Instant timestamp = Instant.now();
        public String type;
        public T data;

        public Event( String type, T data ) {
            this.type = requireNonNull( type );
            this.data = requireNonNull( data );
        }
    }

    /**
     *
     */
    public static abstract class EventSink<T> {
        public abstract void handle( Map<Event<Object>,T> events ) throws Exception;
    }

    /**
     *
     */
    public static abstract class EventTransform<I,O>
            implements Function<Event<I>,O> {
    }


    // instance *******************************************

    private List<EventTransform<P,S>> transforms = new ArrayList<>();

    private List<EventSink<S>>      sinks = new ArrayList<>();

    private PusherThread            t = new PusherThread();


    public void dispose() {
        if (t != null) {
            t.stopRequested = true;
        }
    }

    public EventCollector<P,S> addTransform( EventTransform<P,S> transform ) {
        transforms.add( transform );
        return this;
    }

    public EventCollector<P,S> addSink( EventSink<S> sink ) {
        sinks.add( sink );
        return this;
    }

    public void publish( String eventType, P payload ) {
        try {
            t.queue.put( new Event<>( eventType, payload ) );
        }
        catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
    }

    /**
     *
     */
    protected class PusherThread extends Thread {

        protected volatile boolean stopRequested;

        protected BlockingQueue<Event<P>> queue = new ArrayBlockingQueue<>( 4096 );

        public PusherThread() {
            super( "EventCollector.Pusher" );
            setDaemon( true );
            setPriority( MIN_PRIORITY );
            start();
            LOG.info( "PusherThread started");
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            while (!stopRequested) {
                try {
                    var chunk = new HashMap<Event<Object>,S>( CHUNK_SIZE_MAX * 2 );
                    for (var ev = queue.take(); ev != null && chunk.size() <= CHUNK_SIZE_MAX; ev = queue.poll( INTERVAL.toMillis(), MILLISECONDS )) {
                        //LOG.debug( "Event taken: %s", event );
                        var _ev = ev;
                        chunk.put( (Event<Object>)_ev, transforms.stream()
                                .map( transform -> transform.apply( _ev ) )
                                .filter( transformed -> transformed != null )
                                .findAny().orElseThrow( () -> new RuntimeException( "No transformer found for: " + _ev ) ) );
                    }
                    for (var sink : sinks) {
                        sink.handle( chunk );
                    }
                    LOG.debug( "Events pushed: %s", chunk.size() );
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
