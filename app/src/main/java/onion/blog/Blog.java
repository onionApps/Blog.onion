/*
 * Blog.onion
 *
 * http://play.google.com/store/apps/details?id=onion.blog
 * http://onionapps.github.io/Blog.onion/
 * http://github.com/onionApps/Blog.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.blog;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.text.Html;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Blog {

    private static Charset utf8 = Charset.forName("UTF-8");
    ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

    String respage;
    Context context;
    File dir;
    String[] styles = null;

    private Blog(Context context) {
        this.context = context;
        dir = new File(context.getFilesDir(), "blog");
        if (!dir.exists()) {
            dir.mkdirs();
            setStyle("Material");
            addPost("My First Blog Post", rawstr(R.raw.blind), null);
        }
        respage = rawstr(R.raw.page);
    }

    public static Blog getInstance(Context context) {
        return new Blog(context.getApplicationContext());
    }

    private static byte[] bin(InputStream is) throws IOException {
        return Utils.bin(is);
    }

    static Bitmap createScaledBitmap2(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        Log.i("createScaledBitmap2", dstWidth + " " + dstHeight);
        Matrix m = new Matrix();
        m.setScale(dstWidth / (float) src.getWidth(), dstHeight / (float) src.getHeight());
        Bitmap result = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setFilterBitmap(filter);
        canvas.drawBitmap(src, m, paint);
        return result;

    }

    static Bitmap thumb(Bitmap b) {
        int maxsize = 640;
        if (b.getWidth() > maxsize || b.getHeight() > maxsize) {
            long mx = Math.max(b.getWidth(), b.getHeight());
            return createScaledBitmap2(b, (int) (b.getWidth() * maxsize / mx), (int) (b.getHeight() * maxsize / mx), true);
        } else {
            return null;
        }
    }

    private static String htmlentities(String str) {
        return Html.escapeHtml(str);
    }

    void log(String s) {
        Log.i("Blog", s);
    }

    private byte[] rawbin(int id) {
        try {
            return bin(context.getResources().openRawResource(id));
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    private String rawstr(int id) {
        return new String(rawbin(id), utf8);
    }

    private File file(String path) {
        return new File(dir, path);
    }

    private byte[] filebin(File f) {
        readLock.lock();
        try {
            return Utils.filebin(f);
        } finally {
            readLock.unlock();
        }
    }

    private String filestr(File f) {
        return new String(filebin(f), utf8);
    }

    private String filestr(File f, String def) {
        String s = filestr(f);
        if ("".equals(s.trim())) s = def;
        return s;
    }

    private void strfile(File f, String str) {
        writeLock.lock();
        try {
            FileWriter w = null;
            try {
                w = new FileWriter(f);
                w.write(str);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                if (w != null)
                    w.close();
            } catch (IOException ex) {
            }
        } finally {
            writeLock.unlock();
        }
    }

    public String getTitle() {
        return filestr(file("title.txt"), "Unnamed Blog");
    }

    public void setTitle(String t) {
        strfile(file("title.txt"), t);
    }

    private String assetstrsafe(String name) {
        InputStream is = null;
        try {
            is = context.getAssets().open(name, AssetManager.ACCESS_STREAMING);
            return Utils.str(is);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                }
            }
        }
        return "";
    }

    public void addPost(String title, String text, Bitmap bmp) {
        writeLock.lock();
        try {
            File f = dir;
            f = new File(f, "posts");
            f = new File(f, "" + System.currentTimeMillis() / 1000);
            f.mkdirs();
            strfile(new File(f, "post.txt"), title + "\n" + text);
            if (bmp != null) {
                try {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, new FileOutputStream(new File(f, "img.jpg")));
                } catch (IOException ex) {
                }
                Bitmap t = thumb(bmp);
                if (t != null) {
                    try {
                        t.compress(Bitmap.CompressFormat.JPEG, 85, new FileOutputStream(new File(f, "thm.jpg")));
                    } catch (IOException ex) {
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    private boolean in(String s, String... aa) {
        for (String a : aa) {
            if (a.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private String urlencode(String s) {
        return URLEncoder.encode(s);
    }

    private String date(String str) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new Date(Long.parseLong(str) * 1000));
    }

    private void delete(File f) throws IOException {
        writeLock.lock();
        try {
            if (f.isDirectory()) {
                for (File c : f.listFiles())
                    delete(c);
            }
            if (!f.delete())
                throw new FileNotFoundException("Failed to delete file: " + f);
        } finally {
            writeLock.unlock();
        }
    }

    public void deletePost(String id) throws IOException {
        delete(new File(new File(dir, "posts"), id));
    }

    public void updatePost(String id, String title, String content) {
        strfile(new File(new File(new File(dir, "posts"), id), "post.txt"), title + "\n" + content);
    }

    public PostInfo getPostInfo(String id) {
        readLock.lock();
        try {
            if (!id.matches("^[0-9]+$")) return null;
            File pd = new File(new File(dir, "posts"), id);
            if (!pd.isDirectory()) return null;
            File pf = new File(pd, "post.txt");
            if (!pf.isFile()) return null;
            String d = filestr(pf);
            String[] ll = d.split("\n", 2);
            return new PostInfo(
                    ll.length > 0 ? ll[0] : "",
                    ll.length > 1 ? ll[1] : ""
            );
        } finally {
            readLock.unlock();
        }
    }

    private Post getPost(String pn, boolean edit, boolean fullpage) {

        readLock.lock();
        try {

            if (!pn.matches("^[0-9]+$")) return null;

            StringWriter w = new StringWriter();

            File pd = new File(new File(dir, "posts"), pn);
            if (!pd.isDirectory()) return null;
            File pf = new File(pd, "post.txt");
            if (!pf.isFile()) return null;
            String d = filestr(pf);
            String[] ll = d.split("\n");

            w.write("<div class=\"p pn\">");

            {
                w.write("<p class=\"pi\">");
                w.write(date(pn));

                if (edit) {
                    w.write(" <a class=\"act\" href=\"javascript:cms.edit('" + pn + "')\">[ed]</a>");
                    w.write(" <a class=\"act\" href=\"javascript:cms.delete('" + pn + "')\">[rm]</a>");
                }

                w.write("</p>");
            }

            String id = "post";
            if (ll.length > 0 && !ll[0].trim().equals("")) {
                id = ll[0];
                String[] iidd = id.split("[^a-zA-Z0-9]");
                id = "";
                for (String x : iidd) {
                    if (id.length() > 0) id += "-";
                    id += x;
                }
            }
            id += ".htm";

            String link = "/" + urlencode(pn) + "/" + id;

            //if(ll.length == 0) return null;
            if (ll.length > 0 && !ll[0].trim().equals("")) {
                w.write("<a");
                if (!fullpage) w.write(" href=\"" + link + "\"");
                w.write(" class=\"pt\">" + htmlentities(ll[0]) + "</a>");
            }

            if (new File(pd, "img.jpg").isFile()) {
                String bmp = "/" + urlencode(pn) + "/img.jpg";
                if (!fullpage) {
                    bmp = link;
                }
                String thm = new File(pd, "thm.jpg").isFile() ? ("/" + urlencode(pn) + "/thm.jpg") : bmp;
                w.write("<p class=\"pp\"><a href=\"" + bmp + "\"><img src=\"" + thm + "\"></a></p>");
            }

            for (int i = 1; i < ll.length; i++) {
                if (!ll[i].trim().equals("")) {
                    w.write("<p class=\"pp\">" + htmlentities(ll[i]) + "</p>");
                }
            }

            w.write("</div>");

            Post post = new Post();
            post.title = ll.length > 0 ? ll[0] : "";
            post.html = w.toString();
            return post;

        } finally {
            readLock.unlock();
        }
    }

    public String getStyle() {
        return filestr(file("style.txt"));
    }

    public void setStyle(int id) {
        setStyle(getStyles()[id]);
    }

    public String[] getStyles() {
        if (styles == null) {
            synchronized (this) {
                if (styles == null) {
                    try {
                        String[] files = context.getAssets().list("styles");
                        String[] names = new String[files.length];
                        for (int i = 0; i < files.length; i++) {
                            names[i] = files[i].substring(0, files[i].length() - 4);
                        }
                        styles = names;
                    } catch (IOException ex) {
                        throw new Error(ex);
                    }
                }
            }
        }
        return styles;
    }

    public int getStyleIndex() {
        int i = 0;
        String[] styles = getStyles();
        for (int j = 0; j < styles.length; j++) {
            if (styles[j].equals(getStyle())) {
                i = j;
            }
        }
        return i;
    }

    public void setStyle(String style) {
        strfile(file("style.txt"), style);
    }

    public Response getResponse(String p, boolean edit) {

        readLock.lock();
        try {

            byte[] data = null;
            String mime = null;

            String html = null;
            String title = null;


            log(p);

            boolean titlelink = true;

            if (in(p, "", "/", "/index.htm") ||
                    p.matches("/page\\-[0-9]+\\.htm")) {

                titlelink = false;

                html = "";
                title = getTitle();
                String[] ppl = new File(dir, "posts").list();
                if (ppl == null) ppl = new String[0];
                Arrays.sort(ppl);
                StringWriter w = new StringWriter();

                int page = 1;
                if (p.startsWith("/page-")) {
                    try {
                        page = Integer.parseInt(p.replaceAll("[^0-9]", ""));
                    } catch (Exception ex) {
                    }
                }
                page = Math.max(1, page);

                int n = 8;
                int s = (page - 1) * n;

                int c = 0;
                for (int pi = ppl.length - 1; pi >= 0; pi--) {
                    if (c >= s && c < s + n) {
                        String pn = ppl[pi];
                        Post post = getPost(pn, edit, false);
                        if (post != null)
                            w.write(post.getHtml());
                    }
                    c++;
                }

                if (page == 1 && ppl.length == 0) {
                    w.write("<div class=\"p\">");
                    w.write("<a class=\"pt\">No posts yet.</a>");
                    //w.write("<p class=\"pp\">Nothing has been posted to this blog yet.</p>");
                    //w.write("<p class=\"pp\">No posts yet</p>");
                    w.write("</div>");
                }

                w.write("<div class=\"p\">");
                w.write("Page: ");
                /*if(page != 1) {
                    w.write("<a class=\"pg\" href=\"/\">first</a> ");
                }*/
                int pn = (ppl.length + n - 1) / n;
                if (pn <= 0) pn = 1;
                for (int pi = 1; pi <= pn; pi++) {
                    String pl;
                    if (pi == 1)
                        pl = "/";
                    else
                        pl = "/page-" + pi + ".htm";
                    w.write("<a class=\"pg\"");
                    if (pi != page) {
                        w.write(" href=\"" + pl + "\"");
                    }
                    w.write(">" + pi + "</a> ");
                }
                /*if(page != pn) {
                    String pl = "/page-" + (page + 1) + ".htm";
                    w.write("<a class=\"pg\" href=\"" + pl + "\">next &gt;</a> ");
                }*/
                w.write("</div>");

                html = w.toString();
            }


            String[] pp = p.split("/");
            for (String ppp : pp) {
                log(pp.length + " " + ppp);
            }

            if (pp.length == 3 && pp[0].equals("") && pp[1].matches("^[0-9]+$") && pp[2].matches("^[a-zA-Z0-9-]+\\.htm$")) {
                //log("PAGE");
                String pn = pp[1];
                Post post = getPost(pn, edit, true);
                if (post != null) {
                    html = post.getHtml();
                    title = post.getTitle().equals("") ? getTitle() : (post.getTitle() + " - " + getTitle());
                }
            }

            if (pp.length == 3 && pp[0].equals("") && pp[1].matches("^[0-9]+$") && pp[2].matches("^[a-zA-Z0-9-]+\\.jpg")) {
                //log("IMAGE");
                data = filebin(new File(new File(new File(dir, "posts"), pp[1]), pp[2]));
                mime = "image/jpeg";
            }


            // data response
            if (data != null && mime != null) {
                Response response = new Response();
                response.setData(data);
                response.setMimeType(mime);
                return response;
            }

            // html response
            if (html == null || title == null)
            //if(Boolean.TRUE)
            {
                //html = "<h1>404 Not Found</h1>";
                html = "<div class=\"p\"><h1 class=\"pt\">Error 404: Not Found</h1><p class=\"pp\">The requested URL \"" + htmlentities(p) + "\" was not found on this server.</p></div>";
                title = "404 Not Found";
            }
            {
                String titleedit = "<div style=\"height:0;text-align:right;\">" +
                        "<div style=\"position:relative\">" +
                        "<a style=\"color:#888;\" href=\"javascript:cms.title()\">[ed]</a>" +
                        "</div>" +
                        "</div>";

                String s = respage;

                String t = htmlentities(getTitle());
                if (titlelink) {
                    t = "<a href=\"/\" class=\"bt\">" + t + "</a>";
                } else {
                    t = "<a class=\"bt\">" + t + "</a>";
                }

                // TODO: fix
                s = s.replace("%STYLE%", assetstrsafe("styles/" + getStyle() + ".css"));
                s = s.replace("%EDITTITLE%", edit ? titleedit : "");
                s = s.replace("%BLOGTITLE%", t);
                s = s.replace("%PAGETITLE%", title);
                s = s.replace("%CONTENT%", html);

                html = s;
            }
            Response response = new Response();
            response.setText(html);
            response.setMimeType("text/html");
            return response;

        } finally {
            readLock.unlock();
        }

    }

    private class Post {
        String title;
        String html;

        public String getTitle() {
            return title;
        }

        public String getHtml() {
            return html;
        }
    }

    public class PostInfo {
        String title;
        String content;

        public PostInfo(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }

}
