/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.app.service.http;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class SSLUtils {

    private static final Log LOG = LogFactory.getLog( SSLUtils.class );

    /**
     *
     */
    public static SSLContext relaxSSLContext() throws ServletException {
        try {
            var sslContext = SSLContext.getInstance( "TLS" );
            TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
                }
                @Override
                public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
                }
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            sslContext.init( null, new TrustManager[] { tm }, null );
            return sslContext;
        }
        catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new ServletException("Unable to initialize SSL context: " + e.getMessage(), e );
        }
    }

}
