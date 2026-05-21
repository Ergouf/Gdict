package android.util;

public class Log {
    private static String fmt(int level, String tag, String msg) {
        String lvl = switch (level) {
            case 2 -> "V";
            case 3 -> "D";
            case 4 -> "I";
            case 5 -> "W";
            case 6 -> "E";
            default -> "?";
        };
        return "[" + lvl + "/" + tag + "] " + msg;
    }

    public static int v(String tag, String msg) { System.out.println(fmt(2, tag, msg)); return 0; }
    public static int v(String tag, String msg, Throwable tr) { System.out.println(fmt(2, tag, msg + " " + tr)); return 0; }
    public static int d(String tag, String msg) { System.out.println(fmt(3, tag, msg)); return 0; }
    public static int d(String tag, String msg, Throwable tr) { System.out.println(fmt(3, tag, msg + " " + tr)); return 0; }
    public static int i(String tag, String msg) { System.out.println(fmt(4, tag, msg)); return 0; }
    public static int i(String tag, String msg, Throwable tr) { System.out.println(fmt(4, tag, msg + " " + tr)); return 0; }
    public static int w(String tag, String msg) { System.out.println(fmt(5, tag, msg)); return 0; }
    public static int w(String tag, String msg, Throwable tr) { System.out.println(fmt(5, tag, msg + " " + tr)); return 0; }
    public static int e(String tag, String msg) { System.out.println(fmt(6, tag, msg)); return 0; }
    public static int e(String tag, String msg, Throwable tr) { System.out.println(fmt(6, tag, msg + " " + tr)); return 0; }
    public static int wtf(String tag, String msg) { System.out.println(fmt(6, tag, msg)); return 0; }
    public static int wtf(String tag, String msg, Throwable tr) { System.out.println(fmt(6, tag, msg + " " + tr)); return 0; }
}
