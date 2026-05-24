package com.youssefhenna.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class CertChecker {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    public record CertInfo(Date notBefore, Date notAfter) {
    }

    public static CertInfo checkFirstCert(String host, int port) throws Exception {
        SSLContext sslContext = trustAllSslContext();
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            socket.startHandshake();
            X509Certificate cert = (X509Certificate) socket.getSession().getPeerCertificates()[0];
            return new CertInfo(cert.getNotBefore(), cert.getNotAfter());
        } finally {
            socket.close();
        }
    }

    private static SSLContext trustAllSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, null);
        return ctx;
    }
}