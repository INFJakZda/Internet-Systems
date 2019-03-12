import com.cedarsoftware.util.io.JsonWriter;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TPSIServer {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/echo", new EchoHandler());
        server.createContext("/redirect", new RedirectHandler());
        System.out.println("Starting server on port: " + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();
            os.write(Files.readAllBytes(Paths.get("index.html")));
            os.close();
        }
    }

    static class EchoHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            Headers header = exchange.getRequestHeaders();
            String json = JsonWriter.formatJson(JsonWriter.objectToJson(header));

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(json.getBytes());
            os.close();
        }
    }

    static class RedirectHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String url = exchange.getRequestURI().getPath();
            int status = Integer.parseInt(url.replaceAll("[^-?0-9]+", ""));
            System.out.println(status);

            exchange.getResponseHeaders().set("Location", "http://wp.pl");
            exchange.sendResponseHeaders(status, 0);
            exchange.close();
        }
    }
}