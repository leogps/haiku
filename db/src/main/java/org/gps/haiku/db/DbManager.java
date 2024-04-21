package org.gps.haiku.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by leogps on 12/07/2018.
 */
public interface DbManager {

    void initialize() throws SQLException, ClassNotFoundException;

    void initialize(String dbName) throws SQLException, ClassNotFoundException;

    boolean isInitiated();

    void shutdown() throws SQLException;

    Connection getConnection();

}
