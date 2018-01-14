package es.tm.omar.survival_gps_game;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creado por Omar Torrado Míguez el 14/01/2018
 */

public class SqliteManager {

    private Connection cn=null;

    public int conectar(){
        try {
            Class.forName("org.sqlite.JDBC");
            cn= DriverManager.getConnection("jdbc:sqlite:sur_gps.db");
            return 1;
        } catch (SQLException | ClassNotFoundException ex) {
            return 0;
        }
    }
}
