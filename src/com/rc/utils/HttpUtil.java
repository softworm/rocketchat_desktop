package com.rc.utils;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.*;


public class HttpUtil
{

    public static OkHttpClient client = new OkHttpClient();

    static
    {
        try
        {
            client = initClientBuilder().build();
        }
        catch (KeyManagementException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    public static String get(String url) throws IOException
    {
        return get(url, null, null);
    }

    public static byte[] getBytes(String url, Map<String, String> headers, Map<String, String> params) throws IOException
    {
        try (Response response = _get(url, headers, params))
        {
            if (response != null)
            {
                return response.body().bytes();
            }
            else
            {
                throw new IOException("Get请求失败:" + url);
            }
        }
    }

    public static String get(String url, Map<String, String> headers, Map<String, String> params) throws IOException
    {
        try(Response response = _get(url, headers, params))
        {
            if (response != null)
            {
                return response.body().string();
            }
            else
            {
                throw new IOException("Get请求失败:" + url);
            }
        }
    }

    private static Response _get(String url, Map<String, String> headers, Map<String, String> params) throws IOException
    {
        if (params != null && params.size() > 0)
        {
            StringBuffer buffer = new StringBuffer(url);
            buffer.append("?");
            for (String key : params.keySet())
            {
                buffer.append(key + "=");
                buffer.append(params.get(key) + "&");
            }
            url = buffer.toString();
        }

        Request.Builder reqBuilder = new Request.Builder().url(url);
        if (headers != null && headers.size() > 0)
        {
            for (String key : headers.keySet())
            {
                reqBuilder.addHeader(key, headers.get(key));
            }
        }


        Request request = reqBuilder.build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful())
        {
            return response;
        }
        else
        {
            throw new IOException("Unexpected code " + response);
        }
    }


    public static String post(String url, Map<String, String> headers, Map<String, String> params) throws IOException
    {
        FormBody.Builder builder = new FormBody.Builder();
        for (String key : params.keySet())
        {
            builder.add(key, params.get(key));
        }
        RequestBody requestBodyPost = builder.build();

        Request.Builder reqBuilder = new Request.Builder().url(url);
        if (headers != null && headers.size() > 0)
        {
            for (String key : headers.keySet())
            {
                reqBuilder.addHeader(key, headers.get(key));
            }
        }
        Request requestPost = reqBuilder.post(requestBodyPost).build();

        try(Response response = client.newCall(requestPost).execute())
        {
            return response.body().string();
        }
    }

    public static String postJson(String url, Map<String, String> headers, String json) throws IOException
    {
        RequestBody requestBodyPost = RequestBody.create(MediaType.parse("application/json"), json);

        Request.Builder reqBuilder = new Request.Builder().url(url);
        if (headers != null && headers.size() > 0)
        {
            for (String key : headers.keySet())
            {
                reqBuilder.addHeader(key, headers.get(key));
            }
        }
        Request requestPost = reqBuilder.post(requestBodyPost).build();

        try(Response response = client.newCall(requestPost).execute())
        {
            return response.body().string();
        }
    }

    public static boolean upload(String url, String type, byte[] part) throws IOException
    {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse(type), part))
                .build();

        Response response = client.newCall(request).execute();
        if (response.isSuccessful())
        {
            return true;
        }

        return false;
    }

    public static String uploadFormData(String url, Map<String, String> headers, Map<String, String> params,
                                        String type, File file) throws IOException {
        RequestBody fileBody = RequestBody.create(MediaType.parse(type), file);

        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        builder.addFormDataPart("file", file.getName(), fileBody);
        for (String key : params.keySet())
        {
            builder.addFormDataPart(key, params.get(key));
        }
        RequestBody requestBodyPost = builder.build();

        Request.Builder reqBuilder = new Request.Builder().url(url);
        if (headers != null && headers.size() > 0)
        {
            for (String key : headers.keySet())
            {
                reqBuilder.addHeader(key, headers.get(key));
            }
        }
        Request requestPost = reqBuilder.post(requestBodyPost).build();

        try(Response response = client.newCall(requestPost).execute())
        {
            return response.body().string();
        }
    }

    public static byte[] download(String url) throws IOException
    {
        return download(url, null, null, null);
    }

    public static byte[] download(String url, Map<String, String> headers, Map<String, String> params, ProgressListener listener) throws IOException
    {
        if (params != null && params.size() > 0)
        {
            StringBuffer buffer = new StringBuffer(url);
            buffer.append("?");
            for (String key : params.keySet())
            {
                buffer.append(key + "=");
                buffer.append(params.get(key) + "&");
            }
            url = buffer.toString();
        }

        Request.Builder reqBuilder = new Request.Builder().url(url);
        if (headers != null && headers.size() > 0)
        {
            for (String key : headers.keySet())
            {
                reqBuilder.addHeader(key, headers.get(key));
            }
        }


        Request request = reqBuilder.build();
        byte[] data = null;
        Response response = null;
        try
        {
            response = client.newCall(request).execute();
            if (response.isSuccessful())
            {
                InputStream inputStream = response.body().byteStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                //byte[] buff = new byte[2048000];
                byte[] buff = new byte[2048];

                int len;
                long total = response.body().contentLength();
                //total = response.body().bytes().length;
                //long total = inputStream.available();
                long sum = 0L;
                while ((len = inputStream.read(buff)) > -1)
                {
                    outputStream.write(buff, 0, len);
                    sum += len;

                    if (listener != null)
                    {
                        int progress = (int) (sum * 1.0f / total * 100);
                        listener.onProgress(progress);
                    }

                }

                data = outputStream.toByteArray();

                inputStream.close();
                outputStream.close();
            }
        }
        catch (IOException e)
        {
            throw e;
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }

        return data;
    }

    public static OkHttpClient.Builder initClientBuilder() throws KeyManagementException, NoSuchAlgorithmException
    {
        X509TrustManager xtm = new X509TrustManager()
        {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
            {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
            {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers()
            {
                X509Certificate[] x509Certificates = new X509Certificate[0];
                return x509Certificates;
            }

        };

        SSLContext sslContext = null;
        sslContext = SSLContext.getInstance("SSL");

        sslContext.init(null, new TrustManager[]{xtm}, new SecureRandom());

        HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier()
        {
            @Override
            public boolean verify(String hostname, SSLSession session)
            {
                return true;
            }
        };

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                //.addInterceptor(interceptor)
                .sslSocketFactory(sslContext.getSocketFactory(), xtm)
                .hostnameVerifier(DO_NOT_VERIFY)
                .connectTimeout(10000, TimeUnit.MILLISECONDS);

        return builder;
    }


    public interface ProgressListener
    {
        void onProgress(int process);
    }
}


