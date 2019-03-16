import com.sun.net.httpserver.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileServer {
    private static String root;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Directory path was not provided!");
        } else if (args.length > 1) {
            System.out.println("You can enter only one path!");
        } else {
            root = args[0];
            if (!new File(root).isDirectory()) {
                System.out.println("Specified path is not a directory!");
            } else {
                int port = 8000;
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/", new FileHandler());
                System.out.println("Starting server on port: " + port);
                System.out.println("Root path: " + root);
                server.start();
            }
        }
    }

    static class FileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String url = exchange.getRequestURI().getPath();
            File requestObject = new File(root + url);
            String requestPath = requestObject.getCanonicalPath();
            String response;

            // 404 - File-Dir doesn't exists
            if (!requestObject.exists()) {
                System.out.println("File doesnt exists: " + requestPath);
                response = "404 - File doesn't exists";
                exchange.sendResponseHeaders(404, response.getBytes().length);

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

            // 403 - Path travelsal error
            else if (!requestPath.startsWith(root)) {
                System.out.println("Path travelsal: " + requestPath);
                response = "403 - Access denied";
                exchange.sendResponseHeaders(403, response.getBytes().length);

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

            // is Directory
            else if (requestObject.isDirectory()) {
                System.out.println("Directory: " + requestPath);

                exchange.getResponseHeaders().set("Content-Type", "text/html");

                StringBuilder htmlBuilder = new StringBuilder();
                htmlBuilder.append("<!DOCTYPE html>");
                htmlBuilder.append("<html>");
                htmlBuilder.append("<head><title>" + requestObject.getName() + "</title></head>");
                htmlBuilder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
                htmlBuilder.append("<body>");

                File[] directoryFiles = requestObject.listFiles();

                for (int i = 0; i < directoryFiles.length; i++) {
                    if (directoryFiles[i].isDirectory()) {
                        htmlBuilder.append("<p>Dir: </p>");
                    } else {
                        htmlBuilder.append("<p>File: </p>");
                    }
                    htmlBuilder.append("<a href=\"http://localhost:8000");
                    htmlBuilder.append(url);
                    if (!url.equals("/")) {
                        htmlBuilder.append("/");
                    }
                    htmlBuilder.append(directoryFiles[i].getName());
                    htmlBuilder.append("\">");
                    htmlBuilder.append(directoryFiles[i].getName());
                    htmlBuilder.append("</a><br>");
                }
                htmlBuilder.append("</body>");
                htmlBuilder.append("</html>");
                String html = htmlBuilder.toString();

                exchange.sendResponseHeaders(200, html.length());

                OutputStream os = exchange.getResponseBody();
                os.write(html.getBytes());
                os.close();
            }

            // is File
            else if (requestObject.isFile()) {
                System.out.println("File:      " + requestPath);

                byte[] currentFileBytes = Files.readAllBytes(Paths.get(requestObject.getPath()));
                String type = Files.probeContentType(Paths.get(requestObject.getPath()));

                exchange.getResponseHeaders().set("Content-Type", type);
                //exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + requestObject.getName() + "\"");
                exchange.sendResponseHeaders(200, currentFileBytes.length);

                OutputStream os = exchange.getResponseBody();
                os.write(currentFileBytes);
                os.close();
            }
        }
    }
}