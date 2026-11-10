package ro.demo.usersbackend.sim;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class CustomHttpServer {
	Sim sim;

	public CustomHttpServer(Sim sim) throws IOException {
		this.sim = sim;
		HttpServer server = HttpServer.create(new InetSocketAddress(8888), 0);

		server.createContext("/visualization", this::sendVizualizationData);

		server.start();
		System.out.println("Server started");
	}

	private void sendVizualizationData(HttpExchange exchange) throws IOException {
		if ("GET".equals(exchange.getRequestMethod())) {
			String respose = sim.getJSON();

			exchange.sendResponseHeaders(200, respose.length());

			exchange.getResponseBody().write(respose.getBytes());
			exchange.getResponseBody().close();
		}
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
