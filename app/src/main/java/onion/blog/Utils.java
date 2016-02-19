/*
 * Blog.onion
 *
 * http://play.google.com/store/apps/details?id=onion.blog
 * http://github.com/onionApps/Blog.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.blog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class Utils {

    private static Charset utf8 = Charset.forName("UTF-8");

    public static byte[] bin(InputStream is) throws IOException {
        try {
            byte[] data = new byte[0];
            while (true) {
                byte[] buf = new byte[1024];
                int n = is.read(buf);
                if (n < 0) return data;
                byte[] newdata = new byte[data.length + n];
                System.arraycopy(data, 0, newdata, 0, data.length);
                System.arraycopy(buf, 0, newdata, data.length, n);
                data = newdata;
            }
        } finally {
            is.close();
        }
    }

    public static String str(InputStream is) throws IOException {
        try {
            StringBuilder builder = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(is, "utf-8");
            char[] buffer = new char[1024 * 32]; // TODO: adjust DONE
            while (true) {
                int n = reader.read(buffer);
                if (n <= 0) break;
                builder.append(buffer, 0, n);
            }
            return builder.toString();
        } finally {
            is.close();
        }
    }

    public static byte[] filebin(File f) {
        try {
            return bin(new FileInputStream(f));
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    public static String filestr(File f) {
        return new String(filebin(f), utf8);
    }

}
