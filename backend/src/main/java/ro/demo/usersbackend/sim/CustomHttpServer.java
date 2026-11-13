package ro.demo.usersbackend.sim;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class CustomHttpServer {
	Sim sim;
	HttpServer server;

	public CustomHttpServer(Sim sim) throws IOException {
		this.sim = sim;
		server = HttpServer.create(new InetSocketAddress(8888), 0);

		server.createContext("/sim/visualization", this::sendVizualizationData);
		server.createContext("/sim/picking");

		server.start();
		System.out.println("Server started");
	}

	private void sendVizualizationData(HttpExchange exchange) throws IOException {
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		if (!"GET".equals(exchange.getRequestMethod())) {
			exchange.sendResponseHeaders(405, -1);
			exchange.close();
			return;
		}

		byte[] response = sim.getJSON().getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, response.length);
		exchange.getResponseBody().write(response);
		exchange.close();
	}

	public void closeServer() {
		server.stop(0);
	}
//	private static void handleRequest(HttpExchange exchange) throws IOException {
//		if ("POST".equals(exchange.getRequestMethod())) {
//			String body = new String(exchange.getRequestBody().readAllBytes());
//
//			System.out.println("Received: " + body);
//
//			exchange.sendResponseHeaders(200, 0);
//			exchange.getResponseBody().write("OK".getBytes());
//			exchange.close();
//		}
//	}
}
