package onion.blog;

public class Native {

    static {
        System.loadLibrary("app");
    }

    native public static void killTor();

}
