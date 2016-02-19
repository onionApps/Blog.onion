/*
 * Blog.onion
 *
 * http://play.google.com/store/apps/details?id=onion.blog
 * http://github.com/onionApps/Blog.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.blog;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

public class Server {
    private static Server instance;
    String socketName;
    private Blog blog = null;
    private String TAG = "BlogServer";
    private LocalServerSocket serverSocket;
    private LocalSocket ls;

    public Server(Context context) {
        blog = Blog.getInstance(context);
        log("start listening");
        try {
            socketName = new File(context.getFilesDir(), "socket").getAbsolutePath();
            ls = new LocalSocket();
            ls.bind(new LocalSocketAddress(socketName, LocalSocketAddress.Namespace.FILESYSTEM));
            serverSocket = new LocalServerSocket(ls.getFileDescriptor());
            //s.close();
            socketName = "unix:" + socketName;
            log(socketName);
            //serverSocket = new LocalServerSocket(socketName, LocalServerSocket.Namespace.FILESYSTEM);
        } catch (Exception ex) {
            throw new Error(ex);
        }
        //log(serverSocket.getLocalSocketAddress().getName());
        log("started listening");
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    LocalServerSocket ss = serverSocket;
                    if (ss == null) break;

                    log("waiting for connection");

                    final LocalSocket ls;
                    try {
                        ls = ss.accept();
                    } catch (IOException ex) {
                        //ex.printStackTrace();
                        //continue;
                        throw new Error(ex);
                    }
                    if (ls == null) {
                        log("no socket");
                        continue;
                    }

                    log("new connection");

                    new Thread() {
                        @Override
                        public void run() {

                            handle(ls);

                            try {
                                ls.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }

                        }
                    }.start();
                }
            }
        }.start();
    }

    public static Server getInstance(Context context) {
        if (instance == null) {
            instance = new Server(context);
        }
        return instance;
    }

    private void log(String s) {
        Log.i(TAG, s);
    }

    private void handle(InputStream is, OutputStream os) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        ArrayList<String> headers = new ArrayList<>();
        while (true) {
            String header = r.readLine();
            if (header == null || header.trim().length() == 0) {
                break;
            }
            headers.add(header);
        }

        if (headers.size() == 0) {
            return;
        }

        for (String header : headers) {
            log("Header " + header);
        }

        String[] rr = headers.get(0).split(" ");

        for (String rrr : rr) {
            log("Req " + rrr);
        }

        if (!rr[2].startsWith("HTTP/")) {
            log("Invalid protocol");
            return;
        }

        if (!"GET".equals(rr[0]) && !"HEAD".equals(rr[0])) {
            log("Invalid method");
            return;
        }

        String path = rr[1];

        Response response = blog.getResponse(path, false);
        os.write(("HTTP/1.0 " + response.getStatusCode() + " " + response.getStatusMessage() + "\r\n").getBytes());
        os.write(("Content-Length: " + response.getData().length + "\r\n").getBytes());
        os.write(("Connection: close\r\n").getBytes());
        if (response.getMimeType() != null && response.getCharset() != null) {
            os.write(("Content-Type: " + response.getMimeType() + "; charset=" + response.getCharset() + "\r\n").getBytes());
        } else if (response.getMimeType() != null) {
            os.write(("Content-Type: " + response.getMimeType() + "\r\n").getBytes());
        }
        if (response.getMimeType() != null && !response.getMimeType().equals("text/html")) {
            os.write("Cache-Control: max-age=31556926\r\n".getBytes());
        }
        os.write(("\r\n").getBytes());
        os.write(response.getData());
        os.flush();
    }

    private void handle(LocalSocket s) {
        InputStream is = null;
        OutputStream os = null;

        try {
            is = s.getInputStream();
        } catch (IOException ex) {
        }

        try {
            os = s.getOutputStream();
        } catch (IOException ex) {
        }

        if (is != null && os != null) {
            try {
                handle(is, os);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (os != null) {
            try {
                os.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public String getSocketName() {
        return socketName;
    }
}
