package com.proxy.offshoreProxy;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;

public class OffshoreProxyApplication {
	private static final int PROXY_PORT = 9090;

	public static void main(String[] args) {
		System.out.println("Offshore Proxy listening on port " + PROXY_PORT);

		try (ServerSocket serverSocket = new ServerSocket(PROXY_PORT)) {
			while (true) {
				Socket clientSocket = serverSocket.accept();
				new Thread(() -> handleClientConnection(clientSocket)).start();
			}
		} catch (IOException e) {
			System.err.println("Error starting Offshore Proxy: " + e.getMessage());
		}
	}

	private static void handleClientConnection(Socket clientSocket) {
		try (
				InputStream clientInput = clientSocket.getInputStream();
				OutputStream clientOutput = clientSocket.getOutputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(clientInput))
		) {
			System.out.println("Offshore Proxy is now handling a request from Ship Proxy");

			// Read the request line
			String requestLine = reader.readLine();
			if (requestLine == null || requestLine.isEmpty()) return;

			
			String line;
			while ((line = reader.readLine()) != null && !line.isEmpty()) {
				
			}

			System.out.println("Received request:\n" + requestLine);

			if (requestLine.startsWith("CONNECT")) {
				handleHttpsTunneling(clientSocket, requestLine);
			} else if (requestLine.startsWith("GET")) {
				handleHttpGet(clientOutput, requestLine);
			} else {
				clientOutput.write("HTTP/1.1 405 Method Not Allowed\r\n\r\n".getBytes());
			}

		} catch (IOException e) {
			System.err.println("Error handling client connection: " + e.getMessage());
		}
	}

	private static void handleHttpGet(OutputStream clientOutput, String requestLine) throws IOException {
		String urlString = requestLine.split(" ")[1];

		URL url = new URL(urlString);
		HttpURLConnection connection;

		if (url.getProtocol().equalsIgnoreCase("https")) {
			connection = (HttpsURLConnection) url.openConnection();
		} else {
			connection = (HttpURLConnection) url.openConnection();
		}

		connection.setRequestMethod("GET");
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(5000);

		int responseCode = connection.getResponseCode();
		System.out.println("Received response code: " + responseCode);

		
		String statusLine = "HTTP/1.1 " + responseCode + " " + connection.getResponseMessage() + "\r\n";
		clientOutput.write(statusLine.getBytes());

		
		for (String key : connection.getHeaderFields().keySet()) {
			if (key == null) continue;
			for (String value : connection.getHeaderFields().get(key)) {
				clientOutput.write((key + ": " + value + "\r\n").getBytes());
			}
		}
		clientOutput.write("\r\n".getBytes()); // End of headers

		try (InputStream responseStream = connection.getInputStream()) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = responseStream.read(buffer)) != -1) {
				clientOutput.write(buffer, 0, bytesRead);
			}
		}
	}

	private static void handleHttpsTunneling(Socket clientSocket, String requestLine) {
		String[] parts = requestLine.split(" ");
		String hostPort = parts[1];

		try {
			String[] hostParts = hostPort.split(":");
			String host = hostParts[0];
			int port = Integer.parseInt(hostParts[1]);

			Socket targetSocket = new Socket(host, port);

			OutputStream clientOut = clientSocket.getOutputStream();
			InputStream clientIn = clientSocket.getInputStream();
			OutputStream targetOut = targetSocket.getOutputStream();
			InputStream targetIn = targetSocket.getInputStream();

			// Send 200 Connection Established to client
			clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
			clientOut.flush();

			// Relay traffic in both directions
			Thread t1 = new Thread(() -> relay(clientIn, targetOut));
			Thread t2 = new Thread(() -> relay(targetIn, clientOut));
			t1.start();
			t2.start();

		} catch (Exception e) {
			System.err.println("HTTPS tunnel error: " + e.getMessage());
			try {
				clientSocket.getOutputStream().write("HTTP/1.1 502 Bad Gateway\r\n\r\n".getBytes());
			} catch (IOException ignored) {}
		}
	}

	private static void relay(InputStream in, OutputStream out) {
		try {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				out.flush();
			}
		} catch (IOException ignored) {
		}
	}
}
