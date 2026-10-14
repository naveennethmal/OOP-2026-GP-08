public class Session {

    private static int userId;
    private static String regNo;
    private static String fullName;

    public static void setUser(int id, String reg, String name) {
        userId = id;
        regNo = reg;
        fullName = name;
    }

    public static int getUserId() {
        return userId;
    }

    public static String getRegNo() {
        return regNo;
    }

    public static String getFullName() {
        return fullName;
    }

    public static void clear() {
        userId = 0;
        regNo = null;
        fullName = null;
    }
}