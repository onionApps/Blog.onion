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
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

public class Tor {

    private static String torname = "btor";
    private static String tordirname = "tordata";
    private static String torservdir = "torserv";

    private void log(String s) {
        Log.i("Tor", s);
    }

    private static Tor instance = null;

    public static Tor getInstance(Context context) {
        if (instance == null)
            instance = new Tor(context);
        return instance;
    }

    void ls(File f) {
        log(f.toString());
        if (f.isDirectory()) {
            for (File s : f.listFiles()) {
                ls(s);
            }
        }
    }

    public static String readDomain(Context context) {
        File torsrv = new File(context.getFilesDir(), torservdir);
        return Utils.filestr(new File(torsrv, "hostname")).trim();
    }

    private void extractFile(Context context, int id, String name) {
        try {
            InputStream i = context.getResources().openRawResource(id);
            OutputStream o = context.openFileOutput(name, context.MODE_PRIVATE);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = i.read(buffer)) > 0) {
                o.write(buffer, 0, read);
            }
            i.close();
            o.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            //throw new Error(ex);
        }
    }

    public interface LogListener {
        void onLog();
    }

    private LogListener logListener;

    public void setLogListener(LogListener l) {
        logListener = l;
    }

    private String status = "";

    public String getStatus() {
        return status;
    }

    private boolean ready = false;

    public boolean isReady() {
        return ready;
    }


    public Tor(final Context context) {

        final Server server = Server.getInstance(context);

        new Thread() {
            @Override
            public void run() {
                try {

                    log("kill");
                    Native.killTor();

                    log("install");
                    extractFile(context, R.raw.tor, torname);

                    log("delete on exit");
                    context.getFileStreamPath(torname).deleteOnExit();

                    log("set executable");
                    context.getFileStreamPath(torname).setExecutable(true);

                    log("make dir");
                    File tordir = new File(context.getFilesDir(), tordirname);
                    tordir.mkdirs();

                    log("make service");
                    File torsrv = new File(context.getFilesDir(), torservdir);
                    torsrv.mkdirs();

                    log("configure");
                    PrintWriter torcfg = new PrintWriter(context.openFileOutput("torcfg", context.MODE_PRIVATE));
                    //torcfg.println("Log debug stdout");
                    torcfg.println("Log notice stdout");
                    torcfg.println("DataDirectory " + tordir.getAbsolutePath());
                    torcfg.println("SOCKSPort auto");
                    torcfg.println("HiddenServiceDir " + torsrv.getAbsolutePath());
                    //torcfg.println("HiddenServicePort 80 unix:" + server.getSocketName());
                    torcfg.println("HiddenServicePort 80 " + server.getSocketName());
                    //torcfg.println("HiddenServicePort 80 unix:");
                    torcfg.println();
                    torcfg.close();
                    log(Utils.filestr(new File(context.getFilesDir(), "torcfg")));

                    log("start");
                    Process tor;
                    tor = Runtime.getRuntime().exec(
                            new String[]{
                                    context.getFileStreamPath(torname).getAbsolutePath(),
                                    "-f", context.getFileStreamPath("torcfg").getAbsolutePath()
                            });

                    BufferedReader torreader = new BufferedReader(new InputStreamReader(tor.getInputStream()));
                    while (true) {
                        final String line = torreader.readLine();
                        if (line == null) break;
                        log(line);

                        /*
                        if(line.contains("100%")) {
                            ls(context.getFilesDir());
                            log(Utils.filestr(new File(torsrv, "hostname")));
                        }
                        */

                        status = line;

                        boolean ready2 = ready;

                        if (line.contains("100%")) {
                            ls(context.getFilesDir());
                            log(Utils.filestr(new File(torsrv, "hostname")));
                            ready2 = true;
                        }

                        if (!ready) {
                            ready = ready2;
                            LogListener l = logListener;
                            if (l != null) {
                                l.onLog();
                            }
                        }

                        ready = ready2;
                    }

                } catch (Exception ex) {
                    throw new Error(ex);
                }
            }
        }.start();

    }

}
