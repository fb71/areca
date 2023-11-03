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

import java.util.Base64;
import java.util.Map;

import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import areca.aws.XLogger;
import areca.aws.logs.EventCollector.Event;
import areca.aws.logs.EventCollector.EventSink;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileLocation;

/**
 *
 * @author Falko Bräutigam
 */
public class OpenSearchSink
        extends EventSink<String> {

    private static final XLogger LOG = XLogger.get( OpenSearchSink.class );

    public static final Duration HTTP_TIMEOUT = Duration.ofSeconds( 30 );

    public static final Gson gson = new GsonBuilder() // NOT pretty for AWS :(
            .setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
            .excludeFieldsWithModifiers( Modifier.PRIVATE )
            .create();

    // instance *******************************************

    private String          url = "https://search-logs-jo3eoi2276p" + "" + "hojelclt6ssphva.eu-central-1.es.amaz" + "onaws.com/";

    private Pair<String,String> credentials;

    private HttpClient      http = HttpClient.newBuilder().connectTimeout( HTTP_TIMEOUT ).build();

    private long            idCount = System.currentTimeMillis();


    public OpenSearchSink( String url, Pair<String,String> credentials ) {
//        this.url = url;

        this.credentials = credentials;
        if (credentials == null) {
            LOG.info( "No credentials given. Using ProfileCredentialsProvider." );
            this.credentials = credentials();
        }
    }


    @Override
    public void handle( Map<Event<Object>,String> events ) throws Exception {
        // https://opensearch.org/docs/1.2/opensearch/rest-api/document-apis/bulk/
        // https://docs.aws.amazon.com/opensearch-service/latest/developerguide/gsg.html

        var body = new StringBuilder( 4096 ); // XXX streaming?
        for (var entry : events.entrySet()) {
            body.append( gson.toJson( IndexCommand.create( entry.getKey().type, ""+idCount++ ) ) ).append( "\n" );
            body.append( entry.getValue() ).append( "\n" );
        }
        LOG.debug( "LOG: \n%s", body.toString() );

        String auth = credentials.getLeft() + ":" + credentials.getRight();
        var request = HttpRequest.newBuilder( new URI( url + "_bulk" + "?filter_path=took,errors" ) )
                .method( "POST", BodyPublishers.ofString( body.toString() ) )
                .header( "Content-Type", "application/json" )
                .header( "Authorization", "Basic " + Base64.getEncoder().encodeToString( auth.getBytes() ) )
                .timeout( HTTP_TIMEOUT );

        var response = http.send( request.build(), HttpResponse.BodyHandlers.ofString() );

        if (response.statusCode() >= 300) {
            JsonElement json = JsonParser.parseString( response.body() );
            var pretty = new GsonBuilder().setPrettyPrinting().create().toJson( json );
            LOG.warn( "Wrong response code: %s (%s)", response.statusCode(), pretty );
        }
        else {
            LOG.debug( "Service response: %s", response.body() );
        }
    }


    /**
     *
     */
    protected static class IndexCommand {

        public static IndexCommand create( String _index, String _id ) {
            var result = new IndexCommand();
            var payload = new Payload();
            payload._index = _index;
            payload._id = _id;
            result.create = payload;
            return result;
        }

        public Payload create;
//        public Payload delete;
//        public Payload update;
//        public Payload index;

        static class Payload {
            public String _index;
            public String _id;
        }
    }


    protected static Pair<String,String> credentials() {
//        var provider = ProfileCredentialsProvider.create( "opensearch" );
//        AwsCredentials resolveCredentials = provider.resolveCredentials();
//        this.credentials = ImmutablePair.of( resolveCredentials.accessKeyId(), resolveCredentials.secretAccessKey() );

        var b = ProfileFile.builder();
        ProfileFileLocation.credentialsFileLocation().ifPresent( l -> b.content( l ).type( ProfileFile.Type.CREDENTIALS ) );
        var profile = b.build().profile( "opensearch" )
                .orElseThrow( () -> new RuntimeException( "No [opensearch] profile." ) ).properties();
        return ImmutablePair.of( profile.get( "aws_access_key_id" ), profile.get( "aws_secret_access_key" ) );
    }


    // test ***********************************************

    public static void main( String... args ) throws InterruptedException {
        LOG.info( "%s", credentials() );

//        var logs = new EventCollector<Object,String>()
//                .addTransform( new GsonEventTransformer<Object>() )
//                .addSink( new OpenSearchSink( null, null ) );
//
//        var event = new Ec2InstanceEvent();
//        event.isRunningBefore = true;
//        event.isRunningAfter = false;
//
//        logs.publish( "test", event );
//        Thread.sleep( 5000 );
//        LOG.info( "done");
    }

}
