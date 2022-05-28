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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

import areca.common.base.Opt;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class MatrixClient
        implements JSObject {

    @JSBody(params = { "url" }, script = "return window.matrixcs.createClient( url );")
    public static native MatrixClient create( String url );

    @JSBody(params = { "url", "accessToken", "userId" }, script = "return window.matrixcs.createClient({'baseUrl':url,'accessToken':accessToken,'userId':userId});")
    public static native MatrixClient create( String url, String accessToken, String userId );

    @JSBody(params = {"obj"}, script = "console.log( obj );")
    public static native void console( JSObject obj );

    // instance *******************************************

    @JSMethod
    public abstract void startClient();

    @JSMethod
    public abstract JSPromise<Whoami> whoami();

    /**
     *
     */
    public static abstract class Whoami
            implements JSCommon {

        @JSProperty( "user_id" )
        public abstract String getUserId();

        @JSProperty( "is_guest" )
        public abstract boolean isGuest();

        @JSProperty( "device_id" )
        public abstract OptString deviceId();

        @Override
        public String toString() {
            return String.format( "Whoami[getUserId()=%s, isGuest()=%s, deviceId()=%s]", getUserId(), isGuest(), deviceId() );
        }
    }


    @JSMethod
    public abstract void publicRooms( Callback callback );


    @JSMethod
    public abstract Room[] getRooms();

    /**
     *
     */
    public interface Room
            extends JSCommon<Room> {

        @JSProperty("roomId")
        public String roomId();

        @JSProperty("name")
        public String name();

        @JSProperty("timeline")
        public Timeline[] timeline();

        public default String toString2() {
            return String.format( "Room[name=%s, roomId=%s]", name(), roomId() );
        }
    }

    /**
     *
     */
    public interface Timeline extends JSCommon {

        @JSProperty("event")
        public Event event();
    }


    @JSMethod // client.once('sync', function(state, prevState, res) {
    public abstract void once( String type, Callback3 callback );

    @JSMethod
    public abstract void on( String type, Callback3 callback );

    /**
     *
     */
    public interface Event
            extends JSCommon<Event> {

        @JSProperty("event_id")
        public String eventId();

        @JSProperty("type")
        public String type();

        @JSProperty("sender")
        public String sender();

        @JSProperty("content")
        public JSCommon content();

        public default Opt<Message> messageContent()  {
            return type().equals( "m.room.message" ) ? Opt.of( content().cast() ) : Opt.absent();
        }

        public default String toString2() {
            return String.format( "Event[type=%s, sender=%s]", type(), sender() );
        }
    }


    /**
     *
     */
    public static abstract class Message
            implements JSCommon<Message> {

        @JSProperty
        public abstract OptString getMsgtype();

        @JSProperty
        public abstract OptString getBody();

        @JSProperty
        public abstract OptString getFormat();
    }


    @JSFunctor
    public interface Callback3 extends JSObject {
        void handle( JSCommon first, JSCommon second, JSCommon third );
    }

    @JSFunctor
    public interface Callback extends JSObject {
        void handle( JSObject err, JSObject data );
    }

}
