import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class ProxyServer {

    public static BlackList blackList = new BlackList();

    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        System.out.println("Starting server on port: " + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {
        private HttpURLConnection connection = null;

        private void setConnection(HttpExchange exchange) throws Exception {
            URL url = exchange.getRequestURI().toURL();
            Headers requestHeaders = exchange.getRequestHeaders();

            System.out.println("Połączenie do " + url + " \tTYP: " + exchange.getRequestMethod());

            connection = (HttpURLConnection) url.openConnection();

            // set connection methods
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(exchange.getRequestMethod());
            connection.setRequestProperty("Via", exchange.getLocalAddress().toString());
            connection.setRequestProperty("X-Forwarded-For", exchange.getRemoteAddress().toString());

            // set request headers
            for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
                String headerKey = entry.getKey();
                List<String> headerValues = entry.getValue();
                for (String value : headerValues) {
                    if (headerKey != null)
                        connection.setRequestProperty(headerKey, value);
                }
            }
        }

        private byte[] readAllBytes(InputStream is) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try {
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return buffer.toByteArray();
        }

        private void writeRecievedResponse(HttpExchange exchange) {
            InputStream is;
            byte[] response = null;
            try {
                // check response code - error or not
                if (connection.getResponseCode() < 400) {
                    is = connection.getInputStream();
                } else {
                    is = connection.getErrorStream();
                }

                // read all bytes from response
                if (is.available() > 0)
                    response = readAllBytes(is);

                // set headers from response
                Map<String, List<String>> serverHeaders = connection.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : serverHeaders.entrySet()) {
                    if (entry.getKey() != null && !entry.getKey().equalsIgnoreCase("Transfer-Encoding"))
                        exchange.getResponseHeaders().set(entry.getKey(), entry.getValue().get(0));
                }

                exchange.getResponseHeaders().set("Via", exchange.getLocalAddress().toString());

                // prepare length
                long responseLength = (response != null) ? response.length : -1;

                // prepare response code
                exchange.sendResponseHeaders(connection.getResponseCode(), responseLength);

                // write response
                if (responseLength != -1) {
                    /* write server response to client */
                    OutputStream clientOs = exchange.getResponseBody();
                    clientOs.write(response);
                    clientOs.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!blackList.checkUrl(exchange.getRequestURI().toURL().getHost())) {
                    // establish connection with a server
                    setConnection(exchange);

                    byte[] requestBytes = readAllBytes(exchange.getRequestBody());

                    // when some body data write them
                    if (!exchange.getRequestMethod().equals("GET")) {
                        connection.setDoOutput(true);
                        OutputStream os = connection.getOutputStream();
                        os.write(requestBytes);
                        os.close();
                    }

                    // write response from a server back to user
                    writeRecievedResponse(exchange);
                } else {
                    String response = "BLACK LISTED PAGE !!!";

                    System.out.println("BLACK LIST REQUEST !!!");

                    exchange.sendResponseHeaders(403, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
