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

import areca.common.Promise;
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

    @JSMethod
    public abstract JSRoom[] getRooms();

    @JSMethod // client.once('sync', function(state, prevState, res) {
    public abstract void once( String type, Callback3 callback );

    @JSMethod
    public abstract void on( String type, Callback3 callback );

    @JSMethod
    public abstract void on( String type, Callback2 callback );

    @JSBody(params = {"content"}, script = "return this.crypto.decryptEvent(content);")
    public abstract JSPromise<JSCommon> decrypt( JSCommon content );

    @JSBody(params = {"event"}, script = "return this.crypto.decryptEvent(new window.matrixcs.MatrixEvent(event));")
    public abstract JSPromise<JSCommon> decryptEvent( JSStoredEvent event );

    @JSBody(params = {"event"}, script = "return this.decryptEventIfNeeded(new window.matrixcs.MatrixEvent(event));")
    public abstract JSPromise<JSCommon> decryptEventIfNeeded( JSStoredEvent event );

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
