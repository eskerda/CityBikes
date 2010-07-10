/*
 * Copyright (C) 2010 Lluís Esquerda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.homelinux.penecoptero.android.citybikes.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import android.content.Context;

public class RESTHelper {

	private final String USERNAME;
	private final String PASSWORD;
	private final boolean authenticated;

	public RESTHelper(boolean authenticated, String username,
			String password) {
		this.authenticated = authenticated;
		this.USERNAME = username;
		this.PASSWORD = password;
	}

	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	private static DefaultHttpClient setCredentials(
			DefaultHttpClient httpclient, String url, String username,
			String password) throws HttpException, IOException {
		HttpHost targetHost = new HttpHost(url);
		final UsernamePasswordCredentials access = new UsernamePasswordCredentials(
				username, password);

		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(targetHost.getHostName(), targetHost.getPort()),
				access);

		httpclient.addRequestInterceptor(new HttpRequestInterceptor() {
			@Override
			public void process(HttpRequest request, HttpContext context)
					throws HttpException, IOException {

				AuthState authState = (AuthState) context
						.getAttribute(ClientContext.TARGET_AUTH_STATE);
				if (authState.getAuthScheme() == null) {
					authState.setAuthScheme(new BasicScheme());
					authState.setCredentials(access);
				}
			}
		}, 0);
		return httpclient;
	}

	private DefaultHttpClient setCredentials(DefaultHttpClient httpclient,
			String url) throws HttpException, IOException {
		HttpHost targetHost = new HttpHost(url);
		final UsernamePasswordCredentials access = new UsernamePasswordCredentials(
				USERNAME, PASSWORD);

		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(targetHost.getHostName(), targetHost.getPort()),
				access);

		httpclient.addRequestInterceptor(new HttpRequestInterceptor() {
			@Override
			public void process(HttpRequest request, HttpContext context)
					throws HttpException, IOException {

				AuthState authState = (AuthState) context
						.getAttribute(ClientContext.TARGET_AUTH_STATE);
				if (authState.getAuthScheme() == null) {
					authState.setAuthScheme(new BasicScheme());
					authState.setCredentials(access);
				}
			}
		}, 0);
		return httpclient;
	}

	public String restGET(String url) throws ClientProtocolException,
			IOException, HttpException {

		DefaultHttpClient httpclient = new DefaultHttpClient();
		if (this.authenticated) {
			httpclient = this.setCredentials(httpclient, url);
		}
		// Prepare a request object
		HttpGet httpmethod = new HttpGet(url);

		// Execute the request
		HttpResponse response;

		String result = null;

		try {
			response = httpclient.execute(httpmethod);
			// Get hold of the response entity
			HttpEntity entity = response.getEntity();
			// If the response does not enclose an entity, there is no need
			// to worry about connection release

			if (entity != null) {
				InputStream instream = entity.getContent();
				result = convertStreamToString(instream);
				instream.close();
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	public static final String restGET(String url, boolean authenticated,
			String username, String password) throws ClientProtocolException,
			IOException, HttpException {

		DefaultHttpClient httpclient = new DefaultHttpClient();
		if (authenticated) {
			httpclient = setCredentials(httpclient, url, username, password);
		}
		// Prepare a request object
		HttpGet httpmethod = new HttpGet(url);

		// Execute the request
		HttpResponse response;

		String result = null;

		try {
			response = httpclient.execute(httpmethod);
			// Get hold of the response entity
			HttpEntity entity = response.getEntity();
			// If the response does not enclose an entity, there is no need
			// to worry about connection release

			if (entity != null) {
				InputStream instream = entity.getContent();
				result = convertStreamToString(instream);
				instream.close();
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	public String restPOST(String url, Map<String, String> kvPairs)
			throws ClientProtocolException, IOException, HttpException {

		DefaultHttpClient httpclient = new DefaultHttpClient();
		if (this.authenticated)
			httpclient = this.setCredentials(httpclient, url);

		// Prepare a request object
		HttpPost httpmethod = new HttpPost(url);
		if (kvPairs != null && kvPairs.isEmpty() == false) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
					kvPairs.size());
			String k, v;
			Iterator<String> itKeys = kvPairs.keySet().iterator();
			while (itKeys.hasNext()) {
				k = itKeys.next();
				v = kvPairs.get(k);
				nameValuePairs.add(new BasicNameValuePair(k, v));
			}
			httpmethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		}

		// Execute the request
		HttpResponse response;

		String result = null;

		try {
			response = httpclient.execute(httpmethod);
			// Examine the response status

			// Get hold of the response entity
			HttpEntity entity = response.getEntity();
			// If the response does not enclose an entity, there is no need
			// to worry about connection release

			if (entity != null) {

				InputStream instream = entity.getContent();
				result = convertStreamToString(instream);
				instream.close();
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}
}