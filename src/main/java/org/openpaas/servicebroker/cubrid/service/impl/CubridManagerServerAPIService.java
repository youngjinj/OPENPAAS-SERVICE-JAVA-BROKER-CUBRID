package org.openpaas.servicebroker.cubrid.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openpaas.servicebroker.cubrid.exception.CubridServiceException;
import org.openpaas.servicebroker.exception.ServiceBrokerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CubridManagerServerAPIService {
	private Logger logger = LoggerFactory.getLogger(CubridManagerServerAPIService.class);
	
	private static final String CM_API_URL = "https://45.248.73.54:8001/cm_api";
	// private static final String CM_API_URL = "https://192.168.2.205:8001/cm_api";
	
	private static final String CM_ID = "admin";
	private static final String CM_PASSWORD = "admin";
	private static final String CM_CLIENTVER = "11.0";
	
	private static final String PAGESIZE_16K = "16384";
	private static final String NUM_PAGE_128M = "8192";
	private static final String CUBRID_DATABASES = "/home/cubrid/CUBRID/databases";
	// private static final String CUBRID_DATABASES = "/home/youngjinj/CUBRID/databases";
	private static final String KO_KR_UTF8 = "ko_KR.utf8";
	
	private static String token = null;
	
	public CubridManagerServerAPIService() {
		super();
	}
	
	private HttpsURLConnection getConnection() {
		HttpsURLConnection connection = null;

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new SecureRandom());
		} catch (KeyManagementException e) {
			logger.error(e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage(), e);
		}

		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});

		URL url = null;
		try {
			url = new URL(CubridManagerServerAPIService.CM_API_URL);
		} catch (MalformedURLException e) {
			logger.error(e.getMessage(), e);
		}

		try {
			connection = (HttpsURLConnection) url.openConnection();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false); 
		connection.setDefaultUseCaches(false);
		
		try {
			connection.setRequestMethod("POST");
		} catch (ProtocolException e) {
			logger.error(e.getMessage(), e);
		}

		connection.setRequestProperty("Accept", "text/plain;charset=utf-8");
		connection.setRequestProperty("Content-type", "application/json");

		try {
			connection.connect();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		return connection;
	}
	
	private void sendRequest(HttpsURLConnection connection, JSONObject request) {
		try (OutputStream outputStream = connection.getOutputStream();
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
				PrintWriter printWriter = new PrintWriter(outputStreamWriter)) {

			printWriter.write(request.toString());
			printWriter.flush();
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private JSONObject recvResponse(HttpsURLConnection connection) {
		try (InputStream inputStream = connection.getInputStream();
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

			StringBuilder response = new StringBuilder();
			
			String readLine = null;
			while ((readLine = bufferedReader.readLine()) != null) {
				response.append(readLine);
			}
			
			return (new JSONObject(response.toString()));
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		return null;
	}
	
	private String getErrorMessage(String request, String dbname, JSONObject response) {
		StringJoiner message = new StringJoiner(" ");

		message.add("[").add("CubridManagerServerAPIService").add("-").add(request + " (" + dbname + ")").add("]");

		if (response != null && response.get("task") != null) {
			message.add(response.get("task").toString()).add(":");
		}

		if (response != null && response.get("status") != null) {
			message.add(response.get("status").toString());
		}

		if (response != null && response.get("note") != null) {
			message.add("-").add(response.get("note").toString());
		}

		return message.toString();
	}
	
	private void doLogin() {
		logger.info("Login to CUBRID Manager Server...");
		
		final String task = "login";
		
		HttpsURLConnection connection = getConnection();
		
		JSONObject request = new JSONObject()
				.put("task", task)
				.put("id", CubridManagerServerAPIService.CM_ID)
				.put("password", CubridManagerServerAPIService.CM_PASSWORD)
				.put("clientver", CubridManagerServerAPIService.CM_CLIENTVER);
		
		sendRequest(connection, request);
		logger.info(request.toString(4));
		
		JSONObject response = recvResponse(connection);
		logger.info(response.toString(4));
		
		connection.disconnect();
		logger.info("connection.disconnect()");
		
		CubridManagerServerAPIService.token = response.get("token").toString();
		
		logger.info("Login to CUBRID Manager Server complete.");
	}

	public String doStartinfo(String dbname) {
		logger.info("Getting start info... (DB: " + dbname + ")");
		
		final String task = "startinfo";
		
		HttpsURLConnection connection = getConnection();
		doLogin();
		
		String token = CubridManagerServerAPIService.token;
		
		JSONObject request = new JSONObject()
				.put("task", task)
				.put("token", token);	
		
		sendRequest(connection, request);
		logger.info(request.toString(4));
		
		JSONObject response = recvResponse(connection);
		logger.info(response.toString(4));
	
		connection.disconnect();
		logger.info("connection.disconnect()");
		
		if (response != null && response.getJSONArray("activelist") != null) {
			// Check start
			Iterator<Object> iterActivelist = response.getJSONArray("activelist").iterator();
			while (iterActivelist.hasNext()) {
				JSONArray activearray = ((JSONObject) iterActivelist.next()).getJSONArray("active");

				Iterator<Object> iterActive = activearray.iterator();
				while (iterActive.hasNext()) {
					JSONObject active = (JSONObject) iterActive.next();

					if (active.get("dbname") != null && dbname.equals(active.get("dbname"))) {						
						logger.info("Getting start info complete. : Start. (DB: " + dbname + ")");
						
						return "start";
					}
				}
			}
		}
		
		if (response != null && response.getJSONArray("dblist") != null) {
			// Check create
			Iterator<Object> iterDblist = response.getJSONArray("dblist").iterator();
			while (iterDblist.hasNext()) {
				JSONArray activearray = ((JSONObject) iterDblist.next()).getJSONArray("dbs");

				Iterator<Object> iterDbs = activearray.iterator();
				while (iterDbs.hasNext()) {
					JSONObject active = (JSONObject) iterDbs.next();

					if (active.get("dbname") != null && dbname.equals(active.get("dbname"))) {
						logger.info("Getting start info complete. : Create. (DB: " + dbname + ")");
						
						return "create";
					}
				}
			}
		}
		
		logger.info("Getting start info complete. : Null. (DB: " + dbname + ")");
		
		return null;
	}
	
	public JSONObject doCreatedb(String dbname) {
		logger.info("Creating database... (DB: " + dbname + ")");
		
		final String task = "createdb";
		
		HttpsURLConnection connection = getConnection();
		doLogin();
		
		String numpage = CubridManagerServerAPIService.NUM_PAGE_128M;
		String pagesize = CubridManagerServerAPIService.PAGESIZE_16K;
		String logpagesize = CubridManagerServerAPIService.PAGESIZE_16K;
		String logsize = CubridManagerServerAPIService.NUM_PAGE_128M;
		String genvolpath = CubridManagerServerAPIService.CUBRID_DATABASES + "/" + dbname;
		String logvolpath = CubridManagerServerAPIService.CUBRID_DATABASES + "/" + dbname;
		String charset = CubridManagerServerAPIService.KO_KR_UTF8;
		String overwrite_config_file = "YES";
		
		String token = CubridManagerServerAPIService.token;
		
		JSONObject request = new JSONObject()
				.put("task", task)
				.put("token", token)
				.put("dbname", dbname)
				.put("numpage", numpage)
				.put("pagesize", pagesize)
				.put("logpagesize", logpagesize)
				.put("logsize", logsize)
				.put("genvolpath", genvolpath)
				.put("logvolpath", logvolpath)
				.put("charset", charset)
				.put("overwrite_config_file", overwrite_config_file);

		sendRequest(connection, request);
		logger.info(request.toString(4));
		
		JSONObject response = recvResponse(connection);
		logger.info(response.toString(4));
	
		connection.disconnect();
		logger.info("connection.disconnect()");
		
		logger.info("Creating database complete. (DB: " + dbname + ")");

		return response;
	}
	
	public JSONObject doDeletedb(String dbname) {
		logger.info("Deleting database... (DB: " + dbname + ")");
		
		final String task = "deletedb";
		
		HttpsURLConnection connection = getConnection();
		doLogin();
		
		String delbackup = "y";
		
		String token = CubridManagerServerAPIService.token;
		
		JSONObject request = new JSONObject()
				.put("task", task)
				.put("token", token)
				.put("dbname", dbname)
				.put("delbackup", delbackup);

		sendRequest(connection, request);
		logger.info(request.toString(4));
		
		JSONObject response = recvResponse(connection);
		logger.info(response.toString(4));
	
		connection.disconnect();
		logger.info("connection.disconnect()");
		
		logger.info("Deleting database complete. (DB: " + dbname + ")");

		return response;
	}
	
//	public JSONObject doAddvoldb(String dbname, String volname, String purpose, String numberofpages,
//			String size_need_mb) {
//		logger.info("Adding volume database... (DB: " + dbname + ", VOLUME: " + volname + ")");
//		
//		final String task = "addvoldb";
//		
//		HttpsURLConnection connection = getConnection();
//		
//		try {
//			doLogin(task + ":" + dbname);
//		} catch (ServiceBrokerException e) {
//			e.printStackTrace();
//		}
//		
//		String token = CubridManagerServerAPIService.token;
//		
//		JSONObject request = new JSONObject()
//				.put("task", task)
//				.put("token", token)
//				.put("dbname", dbname)
//				.put("volname", volname)
//				.put("purpose", purpose)
//				.put("numberofpages", numberofpages)
//				.put("size_need_mb", size_need_mb);
//
//		sendRequest(connection, request);
//		logger.info(request.toString(4));
//		
//		JSONObject response = recvResponse(connection);
//		logger.info(response.toString(4));
//	
//		connection.disconnect();
//		logger.info("connection.disconnect()");
//
//		synchronized (CubridManagerServerAPIService.tokenRequestList) {
//			CubridManagerServerAPIService.tokenRequestList.remove(task + ":" + dbname);
//		}
//		
//		logger.info("Adding volume database complete. (DB: " + dbname + ", VOLUME: " + volname + ")");
//
//		return response;
//	}
	
	public JSONObject doStartdb(String dbname) {
		logger.info("Starting database... (DB: " + dbname + ")");
		
		final String task = "startdb";
		
		HttpsURLConnection connection = getConnection();
		doLogin();
		
		String token = CubridManagerServerAPIService.token;
		
		JSONObject request = new JSONObject()
				.put("task", task)
				.put("token", token)
				.put("dbname", dbname);
		
		sendRequest(connection, request);
		logger.info(request.toString(4));
		
		JSONObject response = recvResponse(connection);
		logger.info(response.toString(4));
	
		connection.disconnect();
		logger.info("connection.disconnect()");
		
		logger.info("Starting database complete. (DB: " + dbname + ")");

		return response;
	}
	
	public JSONObject doStopdb(String dbname) {
		logger.info("Stopping database... (DB: " + dbname + ")");
		
		final String task = "stopdb";
		
		HttpsURLConnection connection = getConnection();
		doLogin();
		
		String token = CubridManagerServerAPIService.token;
		
		JSONObject request = new JSONObject()
				.put("task", task)
				.put("token", token)
				.put("dbname", dbname);
		
		sendRequest(connection, request);
		logger.info(request.toString(4));
		
		JSONObject response = recvResponse(connection);
		logger.info(response.toString(4));
	
		connection.disconnect();
		logger.info("connection.disconnect()");
		
		logger.info("Stopping database complete. (DB: " + dbname + ")");

		return response;
	}
	
	public JSONObject doUserinfo(String dbname, String dbapass) {
		logger.info("Getting user info... (DB: " + dbname + ")");
		
		final String task = "userinfo";
		
		HttpsURLConnection connection = getConnection();
		doLogin();
		
		String token = CubridManagerServerAPIService.token;
		
		JSONObject request = new JSONObject()
				.put("task", task)
				.put("token", token)
				.put("dbname", dbname)
				.put("_DBPASSWD", dbapass);

		sendRequest(connection, request);
		logger.info(request.toString(4));
		
		JSONObject response = recvResponse(connection);
		logger.info(response.toString(4));
	
		connection.disconnect();
		logger.info("connection.disconnect()");
		
		logger.info("Getting user info complete. (DB: " + dbname + ")");

		return response;
	}
}
