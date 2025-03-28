package com.proxy.offshoreProxy.offshoreProxyApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class OffshoreProxyApplication {
	private static final int SERVER_PORT = 9090;
	private static final RestTemplate restTemplate = new RestTemplate();
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

	public static void main(String[] args) {
		SpringApplication.run(OffshoreProxyApplication.class, args);
		startProxyServer();
	}

	private static void startProxyServer() {
		try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
			System.out.println("Offshore Proxy Server started on port " + SERVER_PORT);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				threadPool.submit(() -> handleClient(clientSocket));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void handleClient(Socket clientSocket) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

			while (true) {
				StringBuilder requestBuilder = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null && !line.isEmpty()) {
					requestBuilder.append(line).append("\n");
				}

				if (requestBuilder.length() == 0) continue;

				String request = requestBuilder.toString();
				System.out.println("Received request: \n" + request);

				String[] requestLines = request.split("\n");
				String[] requestParts = requestLines[0].split(" ");
				String method = requestParts[0];
				String requestUrl = requestParts[1];

				HttpHeaders headers = new HttpHeaders();
				HttpEntity<String> entity = new HttpEntity<>(null, headers);
				ResponseEntity<String> response = restTemplate.exchange(new URI(requestUrl), HttpMethod.valueOf(method), entity, String.class);

				out.println("HTTP/1.1 200 OK\nContent-Length: " + response.getBody().length() + "\n\n" + response.getBody());
				out.flush();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
