import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class ProxyServer {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        System.out.println("Starting server on port: " + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            URL url = exchange.getRequestURI().toURL();
            String method = exchange.getRequestMethod();
            System.out.println(url.toString() + " " + method);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setAllowUserInteraction(true);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setFollowRedirects(false);
            connection.setRequestMethod(method);

            Headers headers = exchange.getRequestHeaders();

            for (String headerName : headers.keySet()){
                for (String headerValue : headers.get(headerName)){
                    connection.setRequestProperty(headerName, headerValue);
//                    System.out.println(headerName + " " + headerValue);
                }
            }

            if (exchange.getRequestHeaders().containsKey("Content-Length")) {
                if (Integer.parseInt(exchange.getRequestHeaders().get("Content-Length").get(0))>=0) {
                    IOUtils.copy(exchange.getRequestBody(), connection.getOutputStream());
                    System.out.println(exchange.getRequestBody());
                }
            }

            connection.connect();
            System.out.println("Start connect to " + url.toString());


            //getting the response
            //headers
            Map<String, List<String>> responseHeaders = connection.getHeaderFields();
            for (String headerName : responseHeaders.keySet()) {
                if (headerName != null) {
                    exchange.getRequestHeaders().put(headerName, responseHeaders.get(headerName));
                }
            }


            //response code
            int contentLength = 0;
            try {
                if (responseHeaders.containsKey("Content-Length")) {
                    String contentLengthStr = responseHeaders.get("Content-Length").get(0);
                    contentLength = Integer.parseInt(contentLengthStr);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(connection.getResponseCode());
            System.out.println(connection.getInputStream());

            exchange.sendResponseHeaders(connection.getResponseCode(), contentLength);
            System.out.println(connection.getResponseCode());

            //message body
            if (200 <= connection.getResponseCode() && connection.getResponseCode() < 300) {
                IOUtils.copy(connection.getInputStream(),exchange.getResponseBody());
            } else {
                IOUtils.copy(connection.getErrorStream(),exchange.getResponseBody());
            }
            connection.getInputStream().close();
            exchange.getResponseBody().close();
            connection.disconnect();
            exchange.close();

        }
    }
}
