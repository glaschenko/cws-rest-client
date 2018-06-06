package com.haulmont.cubastore.restclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


/*
 * Author: glaschenko
 * Created: 13.04.2018
 */
public class StoreRestClient {
    public static final String ENC = "UTF-8";

    private String accessToken;

    public static void main(String[] args) {
        StoreRestClient client = new StoreRestClient();
        try {
            client.login();
            client.getInvoiceItems();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Logs in with a user name and password and saves the access token for subsequent REST API invocations.
     */
    private void login() throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
//            HttpPost post = new HttpPost("https://www.cuba-platform.com/market/management/rest/v2/oauth/token");
            HttpPost post = new HttpPost("http://localhost:8080/app/rest/v2/oauth/token");

            // see cuba.rest.client.id and cuba.rest.client.secret application properties
            String credentials = Base64.getEncoder().encodeToString("client:secret".getBytes(ENC));
            post.setHeader("Authorization", "Basic " + credentials);

            // user credentials
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "password"));
            params.add(new BasicNameValuePair("username", "admin"));
            params.add(new BasicNameValuePair("password", "admin"));
            post.setEntity(new UrlEncodedFormEntity(params));

            System.out.println("Executing request " + post.getRequestLine());

            String json = httpclient.execute(post, new StringResponseHandler());
            JSONObject jsonObject = new JSONObject(json);
            accessToken = jsonObject.getString("access_token");

            System.out.println("Logged in, session id: " + accessToken);
        }
    }

    /**
     * Creates a Customer by sending JSON to the standard REST API CRUD method.
     */
    private void getInvoiceItems() throws IOException {

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(
                    "http://localhost:8080/app/rest/v2/queries/cubawebsiteback$Invoice/invoices-query?lastCheck=2018-04-10&bespoke=true");
            get.setHeader("Authorization", "Bearer " + accessToken);
            System.out.println("Executing request " + get.getRequestLine());

            CloseableHttpResponse response = httpclient.execute(get);

            String retSrc = EntityUtils.toString(response.getEntity());
            JSONArray array = new JSONArray(retSrc);

            System.out.println("Response: " + array.toString(2));
        }
    }

    private static class StringResponseHandler implements ResponseHandler<String> {
        @Override
        public String handleResponse(HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status + ", msg: " + response.getStatusLine().getReasonPhrase());
            }
        }
    }}
