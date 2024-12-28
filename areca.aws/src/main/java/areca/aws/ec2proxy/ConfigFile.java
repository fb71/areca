/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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
package areca.aws.ec2proxy;

import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.FileReader;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import areca.aws.XLogger;
import areca.aws.logs.ElasticSearchSink;

/**
 *
 * @author Falko Bräutigam
 */
public class ConfigFile {

    private static final XLogger LOG = XLogger.get( ConfigFile.class );

    /**
     * {@link VHost}
     */
    public static class VHostConfig {
        /** The URL server names of the virtual server */
        @Expose
        @SerializedName("hostnames")
        public List<String>     hostnames;

        /** The EC2 instance id */
        @Expose
        @SerializedName("ec2id")
        public String           ec2id;

        @Expose
        @SerializedName("idleTimeout")
        public String           idleTimeout;

        /** The name of the robots.txt resource, or null. */
        @Expose
        public String           robots;

        @Expose
        @SerializedName("proxypaths")
        public List<ProxyPath>  proxypaths;

        public static class ProxyPath {
            @Expose
            @SerializedName("path")
            public String   path;

            @Expose
            @SerializedName("redirect")
            public String   redirect;

            @Expose
            @SerializedName("forward")
            public String   forward;

            /** The URL to be used to check if the instance is running. */
            @Expose
            @SerializedName("ping")
            public String   ping;

            @Expose
            public List<String> allowedServices;
        }
    }

    /**
     * {@link ElasticSearchSink}
     */
    public static class ElasticConfig {

        @Expose
        public String       url;

        @Expose
        public String       user;

        @Expose
        public String       pwd;

        @Expose
        public String       bufferMaxAge;

        @Expose
        public String       bufferMaxSize;
    }

    /**
     * {@link XLogger}
     */
    public static class LogConfig {

        @Expose
        public String       level = "INFO";
    }

    // instance *******************************************

    @Expose
    public List<VHostConfig> vhosts = new ArrayList<>();

    @Expose
    public ElasticConfig    elastic = new ElasticConfig();

    @Expose
    public LogConfig        log = new LogConfig();


    public static ConfigFile read() {
        var home = System.getProperty( "user.home");
        var f = new File( home, "proxy.config" );
        if (!f.exists()) {
            LOG.info( "No config at: %s", f.getAbsolutePath() );
            f = new File( home, ".config/proxy.config" );
            LOG.info( "      trying: %s", f.getAbsolutePath() );
        }
        else if (!f.exists()) {
            throw new RuntimeException( "No config found!" );
        }

        try (var in = new FileReader( f ) ) {
            return new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation().create()
                    .fromJson( in, ConfigFile.class );
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }


    // Test ***********************************************

    public static void main( String[] args ) throws Exception {
        var config = ConfigFile.read();

        for (VHostConfig vhost : config.vhosts) {
            System.out.println( "vhost: " + vhost.hostnames );
        }
        System.out.println( "Elastic: url = " + config.elastic.url );
    }


}
