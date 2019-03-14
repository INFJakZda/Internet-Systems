import com.sun.net.httpserver.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

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

        }
    }
}