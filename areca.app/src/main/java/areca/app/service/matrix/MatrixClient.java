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
import org.teavm.jso.core.JSArray;

import areca.app.ArecaApp;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.WaitFor;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class MatrixClient
        implements JSObject {

    private static final Log LOG = LogFactory.getLog( MatrixClient.class );

    @JSBody(params = { "url" }, script = "return window.matrixcs.createClient( url );")
    public static native MatrixClient create( String url );

    @JSBody(params = { "url", "accessToken", "userId", "deviceId" }, script = "return window.matrixcs.createClient({"
            + "'baseUrl':url,"
            + "'accessToken':accessToken,"
            + "'userId':userId,"
            + "'sessionStore':new window.matrixcs.WebStorageSessionStore(window.localStorage),"
            + "'deviceId':deviceId"
            + "});")
    public static native MatrixClient create( String url, String accessToken, String userId, String deviceId );

    @JSBody(params = {"obj"}, script = "console.log( obj );")
    public static native void console( JSObject obj );


    // instance *******************************************

    private String clientSyncState;


    public abstract JSPromise<JSCredentials> loginWithPassword( String username, String password );

    /** */
    @JSMethod
    public abstract JSPromise<JSCommon> downloadKeys( String[] usersIds );

    /** */
    @JSMethod
    public abstract JSPromise<JSCommon> uploadKeys();

    @JSMethod
    public abstract JSPromise<JSCommon> exportRoomKeys();

    @JSMethod
    public abstract JSPromise<JSCommon> initCrypto();

    /** After {@link #startClient()} */
    @JSMethod
    public abstract void setGlobalErrorOnUnknownDevices( boolean flag );

    @JSMethod
    public abstract JSStoredDevice[] getStoredDevicesForUser( String userId );

    @JSMethod
    public abstract JSPromise setDeviceKnown( String userId, String deviceId, boolean flag );

    @JSMethod
    public abstract JSPromise setDeviceVerified( String userId, String deviceId, boolean flag );

    public void startClient() {
        startClient( 30000 );
    }

    /**
     * @param pollTimeout The number of milliseconds to wait on /sync. Default: 30000 (30 seconds).
     */
    @JSBody(params = {"pollTimeout"}, script = "return this.startClient({'pollTimeout':pollTimeout});")
    public abstract void startClient( int pollTimeout );

    @JSMethod
    public abstract void stopClient();

    public Promise<MatrixClient> waitForStartup() {
        once( "sync", (_state, prevState, res) -> {
            OptString state = _state.cast();
            clientSyncState = state.opt().orElse( null );
            LOG.info( "Client sync: %s - %s", clientSyncState, "PREPARED".equals( clientSyncState ) );
        });
        return new WaitFor<MatrixClient>( () -> "PREPARED".equals( clientSyncState ) )
                .thenSupply( () -> MatrixClient.this )
                .errorWhen( () -> "ERROR".equals( clientSyncState ) ? new RuntimeException("ERROR state in matrix client") : null )
                .start();
    }

    @JSMethod
    public abstract JSPromise<JSWhoami> whoami();

    @JSMethod
    public abstract void publicRooms( Callback2 callback );

    /**
     * Retrieve all known rooms.
     * <p>
     * <a href="http://matrix-org.github.io/matrix-js-sdk/18.0.0/module-client.MatrixClient.html#getRooms">SDK Doc</a>
     *
     * @return A list of rooms, or an empty list if there is no data store.
     */
    @JSMethod
    public abstract JSRoom[] getRooms();

    /**
     * Get the room for the given room ID. This function will return a valid room for
     * any room for which a Room event has been emitted. Note in particular that
     * other events, eg. RoomState.members will be emitted for a room before this
     * function will return the given room.
     * <p>
     * <a href=
     * "http://matrix-org.github.io/matrix-js-sdk/18.0.0/module-client.MatrixClient.html#getRoom">SDK
     * Doc</a>
     *
     * @return The Room or null if it doesn't exist or there is no data store.
     */
    @JSMethod
    public abstract JSRoom getRoom( String roomId );

    /**
     * <a href="http://matrix-org.github.io/matrix-js-sdk/18.0.0/module-client.MatrixClient.html#getUser">SDK Doc</a>
     */
    @JSMethod
    public abstract JSUser getUser( String userId );

    /**
     * <a href="http://matrix-org.github.io/matrix-js-sdk/18.0.0/module-client.MatrixClient.html#getUsers">SDK Doc</a>
     */
    @JSMethod
    public abstract JSUser[] getUsers();

    @JSMethod
    public abstract JSPromise<JSArray<JSCommon>> getJoinedRoomMembers( String roomId );

    /**
     * <a href="http://matrix-org.github.io/matrix-js-sdk/18.0.0/module-client.MatrixClient.html#getUsers">SDK Doc</a>
     *
     * @param info The kind of info to retrieve: 'displayname' or 'avatar_url' or null.
     */
    @JSMethod
    public abstract JSPromise<JSProfileInfo> getProfileInfo( String userId, String info );

    public Promise<JSProfileInfo> getProfileInfoNoError( String userId ) {
        var result = new Completable<JSProfileInfo>();
        getProfileInfo( userId, null )
                .then( (JSProfileInfo value) -> result.complete( value ) )
                .catch_( err -> result.complete( err.cast() ) );
        return result;
    }

    public interface JSProfileInfo extends JSObject {

        @JSProperty( "errcode" )
        public OptString errorCode();

        @JSProperty( "displayname" )
        public String displayName();

        @JSProperty( "avatar_url" )
        public String avatarUrl();
    }

    /**
     * Turn an MXC URL into an HTTP one. This method is experimental and may change.
     *
     * @param mxc The MXC URL.
     * @param width The desired width of the thumbnail.
     * @param height The desired height of the thumbnail.
     * @param resizeMethod The thumbnail resize method to use, either "crop" or
     *        "scale".
     * @param allowDirectLinks If true, return any non-mxc URLs directly. Fetching
     *        such URLs will leak information about the user to anyone they share a
     *        room with. If false, will return null for such URLs.
     * @return The (wrong {@link ArecaApp#proxiedUrl(String) proxied}) HTTP URL, or null.
     */
    @JSMethod
    public abstract String mxcUrlToHttp( String mxc, int width, int height, String resizeMethod, boolean allowDirectLinks );

    @JSMethod
    public abstract void once( String type, Callback3 callback );

    @JSMethod
    public abstract void on( String type, Callback3 callback );

    @JSMethod
    public abstract void on( String type, Callback2 callback );

    @JSMethod
    public abstract void on( String type, Callback1 callback );

//    @JSBody(params = {"content"}, script = "return this.crypto.decryptEvent(content);")
//    public abstract JSPromise<JSCommon> decrypt( JSCommon content );
//
//    @JSBody(params = {"event"}, script = "return this.crypto.decryptEvent(new window.matrixcs.MatrixEvent(event));")
//    public abstract JSPromise<JSCommon> decryptEvent( JSStoredEvent event );
//
//    @JSBody(params = {"event"}, script = "return this.decryptEventIfNeeded(new window.matrixcs.MatrixEvent(event));")
//    public abstract JSPromise<JSCommon> decryptEventIfNeeded( JSStoredEvent event );

    @JSMethod
    public abstract JSPromise<JSCommon> decryptEventIfNeeded( JSEvent event );

    @JSMethod
    public abstract JSPromise<JSCommon> sendEvent( String roomId, String eventType, JSMessage content, String txnId );

//    const content = {
//            "body": "message text",
//            "msgtype": "m.text"
//        };
//        client.sendEvent("roomId", "m.room.message", content, "", (err, res) => {
//            console.log(err);
//        });

    @JSFunctor
    public interface Callback3 extends JSObject {
        void handle( JSCommon _1, JSCommon _2, JSCommon _3 );
    }

    @JSFunctor
    public interface Callback2 extends JSObject {
        void handle( JSCommon _1, JSCommon _2 );
    }

}
