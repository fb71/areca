/*
 * Copyright (C) 2023-2024, the @authors. All rights reserved.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.ONE_GB;
import static org.apache.commons.io.FileUtils.ONE_KB;
import static org.apache.commons.io.FileUtils.ONE_MB;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Map;
import java.util.regex.Pattern;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import areca.aws.XLogger;
import areca.aws.ec2proxy.ConfigFile;
import areca.aws.ec2proxy.SSLUtils;
import areca.aws.logs.EventCollector.Event;
import areca.aws.logs.EventCollector.EventSink;

/**
 *
 * @author Falko Bräutigam
 */
public class ElasticSearchSink
        extends EventSink<String> {

    static final XLogger LOG = XLogger.get( ElasticSearchSink.class );

    public static final Duration    HTTP_TIMEOUT = Duration.ofSeconds( 90 );

    public static final Gson        gson = new GsonBuilder() // NOT pretty for AWS :(
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .excludeFieldsWithModifiers( Modifier.PRIVATE )
            .create();

    private static final byte[]     EOL = "\n".getBytes( UTF_8 );

    private static final Encoder    BASE64 = Base64.getEncoder();

    private static final Pattern    SIZE = Pattern.compile( "([0-9]+)([mMGgKk])" );

    // instance *******************************************

    private String          url;

    private HttpClient      http = HttpClient.newBuilder().connectTimeout( HTTP_TIMEOUT ).build();

    private long            idCount = System.currentTimeMillis();

    private File            buffer = new File( "ElasticSearchSink.buffer" );

    private Duration        bufferMaxAge;

    /** in MB */
    private long            bufferMaxSize;

    private String          basicAuth;


    public ElasticSearchSink( ConfigFile config ) {
        this.url = config.elastic.url;
        this.bufferMaxAge = Duration.parse( config.elastic.bufferMaxAge );

        var m = SIZE.matcher( config.elastic.bufferMaxSize );
        if (!m.matches()) {
            throw new RuntimeException( "Unable to parse: " + config.elastic.bufferMaxSize );
        }
        this.bufferMaxSize = switch (m.group( 2 ).toUpperCase()) {
            case "G" -> Long.parseLong( m.group( 1 ) ) * ONE_GB ;
            case "M" -> Long.parseLong( m.group( 1 ) ) * ONE_MB;
            case "K" -> Long.parseLong( m.group( 1 ) ) * ONE_KB;
            default -> throw new RuntimeException( "Unexpected value: " + m.group( 2 ) );
        };

        var auth = config.elastic.user + ":" + config.elastic.pwd;
        this.basicAuth = "Basic " + BASE64.encodeToString( auth.getBytes() );

        this.http = HttpClient.newBuilder()
                .sslContext( SSLUtils.trustAllSSLContext() )
                .connectTimeout( HTTP_TIMEOUT ).build();

        LOG.warn( "Buffer: %s", buffer.getAbsolutePath() );
        LOG.warn( "   %sh : %s", bufferMaxAge.toHours(), bufferMaxSize );
    }


    @Override
    public void handle( Map<Event<Object>,String> events ) throws Exception {
        LOG.info( "events: %s", events.size() );
        try (var out = new BufferedWriter( new FileWriter( buffer, UTF_8, true ) )) {
            for (var entry : events.entrySet()) {
                if (!entry.getValue().contains( url )) {
                    Event<Object> ev = entry.getKey();
                    out.append( IndexCommand.create( ev.type, idCount++ ).json() ).append( "\n" );
                    out.append( entry.getValue() ).append( "\n" );
                }
                else {
                    LOG.warn( "Skipping: %s", entry.getValue() );
                }
            }
        }
        //
        var attr = Files.readAttributes( buffer.toPath(), BasicFileAttributes.class);
        LOG.info( "BUFFER: %s : %s : %s", attr.creationTime(), System.currentTimeMillis() - attr.creationTime().toMillis(), bufferMaxAge.toMillis() );
        var isTooOld = (attr.creationTime().toMillis() + bufferMaxAge.toMillis()) < System.currentTimeMillis();
        var isTooBig = buffer.length() > bufferMaxSize;
        if (isTooBig || isTooOld) {
            flushBuffer();
        }
    }


    protected void flushBuffer() throws Exception {
        LOG.warn( "Sending: %s bytes", buffer.length() );
        var request = HttpRequest.newBuilder( new URI( url + "_bulk" + "?filter_path=took,errors" ) )
                .method( "POST", BodyPublishers.ofFile( buffer.toPath() ) )
                .header( "Content-Type", "application/json" )
                .header( "Authorization", basicAuth )
                .timeout( HTTP_TIMEOUT );

        var response = http.send( request.build(), HttpResponse.BodyHandlers.ofString() );

        if (response.statusCode() >= 300) {
            LOG.warn( "Error: %s", response.body() );
            JsonElement json = JsonParser.parseString( response.body() );
            var pretty = new GsonBuilder().setPrettyPrinting().create().toJson( json );
            LOG.warn( "Wrong response code: %s (%s)", response.statusCode(), pretty );
        }
        else {
            LOG.info( "Response (ElasticSearch): %s", response.body() );
            Files.delete( buffer.toPath() );
        }
    }


    /**
     *
     */
    protected static class IndexCommand {
        // https://opensearch.org/docs/1.2/opensearch/rest-api/document-apis/bulk/
        // https://docs.aws.amazon.com/opensearch-service/latest/developerguide/gsg.html

        public static IndexCommand create( String _index, long _id ) {
            return create( _index, String.valueOf( _id ) );
        }

        public static IndexCommand create( String _index, String _id ) {
            var result = new IndexCommand();
            var payload = new Payload();
            payload._index = _index;
            payload._id = _id;
            result.create = payload;
            return result;
        }

        public String json() {
            return gson.toJson( this );
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


    // test ***********************************************

    public static void main( String... args ) throws Exception {
        LOG.info( "ElasticSearchSink" );

        var config = ConfigFile.read();
        var test = new ElasticSearchSink( config );
        LOG.info( "credentials: %s:%s", config.elastic.user, config.elastic.pwd );

        var query = "{ \n"
                + "    \"query\" : { \n"
                + "        \"match_all\" : {} \n"
                + "    },\n"
                + "    \"fields\": [\"\"]\n"
                + "}";

        var request = HttpRequest.newBuilder( new URI( test.url + "_search?pretty=true" ) )
                .method( "POST", BodyPublishers.ofString( query ) )
                .header( "Content-Type", "application/json" )
                .header( "Authorization", test.basicAuth )
                .timeout( HTTP_TIMEOUT );

        var http = test.http; //HttpClient.newBuilder().connectTimeout( HTTP_TIMEOUT ).build();
        var response = http.send( request.build(), HttpResponse.BodyHandlers.ofString() );

        if (response.statusCode() >= 300) {
            JsonElement json = JsonParser.parseString( response.body() );
            var pretty = new GsonBuilder().setPrettyPrinting().create().toJson( json );
            LOG.warn( "Wrong response code: %s (%s)", response.statusCode(), pretty );
        }
        else {
            LOG.warn( "Response (OpenSearch): %s", response.body() );
        }



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
