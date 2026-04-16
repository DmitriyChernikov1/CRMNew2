import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConfig {

    public static final String URL  =
            "jdbc:postgresql://your-host:5432/your-db";

    public static final String USER = "your-user";
    public static final String PASS = "your-password";

    // Получить соединение одной строкой
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}