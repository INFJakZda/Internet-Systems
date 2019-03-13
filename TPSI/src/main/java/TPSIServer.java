import com.cedarsoftware.util.io.JsonWriter;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Random;

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
            byte[] array = new byte[7]; // length is bounded by 7
            new Random().nextBytes(array);
            String generatedString = new String(array, Charset.forName("UTF-8"));
            generatedString = "My-Cookie=123456";


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
            Headers header = exchange.getRequestHeaders();
            String auth = header.getFirst("Authorization");
            System.out.println(auth);

            if (auth == null) {
                System.out.println("Null");
                exchange.getResponseHeaders().set("WWW-Authenticate", "Basic");
                exchange.sendResponseHeaders(401, 0);
                exchange.close();
            } else {
                if (!auth.equals("Basic dXNlcjp1")) {
                    exchange.getResponseHeaders().set("WWW-Authenticate", "Basic");
                    exchange.sendResponseHeaders(401, 0);
                    exchange.close();
                } else {
                    basicAuthReq(exchange);
                    return;
                }
            }
        }

        private static void basicAuthReq(HttpExchange exchange) throws IOException {
            String response = "BasicAuth";

            exchange.getResponseHeaders().set("Content-Type", "text");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

//            byte[] decodedBytes = Base64.getDecoder().decode(auth);
//            String decodedString = new String(decodedBytes);
//            System.out.println(decodedString);

//            String base64Credentials = auth.substring("Basic".length()).trim();
//            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
//            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
//            // credentials = username:password
//            final String[] values = credentials.split(":", 2);
//            System.out.println(values[0] + values[1]);

    static class AuthorizedSiteHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            BasicAuthHandler.basicAuthReq(exchange);
        }
    }

    static class MyBasicAuthenticator extends BasicAuthenticator {
        public MyBasicAuthenticator(String realm) {
            super(realm);
        }

        @Override
        public boolean checkCredentials(String username, String pwd) {
            return username.equals("admin") && pwd.equals("admins");
        }
    }
}