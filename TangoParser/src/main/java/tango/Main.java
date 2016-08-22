package tango;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
//        int port = Integer.valueOf(args[0]);
//        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
//        server.createContext("/", new MyHandler());
//        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
//        server.start();
//        System.out.println("server listen on port: " + port);
        try {
            InputStream input = System.in;
            OutputStream output = System.out;
            new Parser(input, output);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class MyHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
        System.out.println("get a request from " + t.getRemoteAddress());
        System.out.println("method: " + t.getRequestMethod());
        System.out.println(t.getRequestHeaders().toString());
        Headers h = t.getRequestHeaders();
        Set hs = t.getRequestHeaders().entrySet();
        Iterator it = hs.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
        InputStream input = t.getRequestBody();
        long filesize = 0;
        int next, rd;
        char ch;
        while ((rd = input.read()) != -1) {
            System.out.print((char)rd);
            filesize += 1;
        }
        System.out.println(filesize);
        t.sendResponseHeaders(200, 0);
        OutputStream output = t.getResponseBody();
//        new Parser(input, output);
        output.close();
    }
}
