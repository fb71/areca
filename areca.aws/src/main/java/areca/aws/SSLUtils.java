package areca.aws;
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


import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;

/**
 *
 * @author Falko Bräutigam
 */
public class SSLUtils {

    /**
     * Always verify the host - dont check for certificate
     */
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     *
     */
    public static SSLContext trustAllSSLContext() throws ServletException {
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
