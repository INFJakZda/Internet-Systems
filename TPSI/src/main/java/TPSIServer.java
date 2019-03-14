import com.cedarsoftware.util.io.JsonWriter;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class TPSIServer {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/echo/", new EchoHandler());
        server.createContext("/redirect/", new RedirectHandler());
        server.createContext("/cookies/", new CookieHandler());
        server.createContext("/auth/", new BasicAuthHandler());
        server.createContext("/auth2/", new AuthorizedSiteHandler())
                .setAuthenticator(new MyBasicAuthenticator("test"));
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

    static class CookieHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String generatedString = "My-Cookie=123456; path=/echo";

            exchange.getResponseHeaders().set("Content-Type", "text");
            exchange.getResponseHeaders().set("Set-Cookie", generatedString);
            exchange.sendResponseHeaders(200, generatedString.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(generatedString.getBytes());
            os.close();
        }
    }

    static class BasicAuthHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");

            Headers headers = exchange.getRequestHeaders();

            if (headers.containsKey("Authorization")){
                String authorizationHeader = headers.getFirst("Authorization");
                String encodedMessage = authorizationHeader.substring(6);
                String decodedMessage = new String(Base64.getDecoder().decode(encodedMessage));

                System.out.println(decodedMessage);
                if (decodedMessage.equals("user:1234")){
                    String response = "Goood!";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
                else {
                    exchange.getResponseHeaders().set("WWW-Authenticate", "Basic");
                    exchange.sendResponseHeaders(401, -1);

                }
            }
            else {
                exchange.getResponseHeaders().set("WWW-Authenticate", "Basic");
                exchange.sendResponseHeaders(401, -1);
            }
        }
    }

    static class AuthorizedSiteHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String response = "BasicAuth";

            exchange.getResponseHeaders().set("Content-Type", "text");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class MyBasicAuthenticator extends BasicAuthenticator {
        public MyBasicAuthenticator(String realm) {
            super(realm);
        }

        @Override
        public boolean checkCredentials(String username, String pwd) {
            return username.equals("user") && pwd.equals("1234");
        }
    }
}