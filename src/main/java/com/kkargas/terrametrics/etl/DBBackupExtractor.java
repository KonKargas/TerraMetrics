package com.kkargas.terrametrics.etl;

import java.nio.file.*;
import java.sql.*;

/** Consistent SQLite backup using VACUUM INTO, never a potentially unsafe live-file copy. */
public final class DBBackupExtractor {
    private DBBackupExtractor() {}
    public static void main(String[] args)throws Exception {
        Path db=Paths.get("data/climate.db"), backup=Paths.get("data/backups/climate-backup.db");
        if(!Files.exists(db))throw new IllegalStateException("Database does not exist: "+db);
        Files.createDirectories(backup.getParent());Files.deleteIfExists(backup);
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+db.toAbsolutePath());Statement s=c.createStatement()) {
            String target=backup.toAbsolutePath().toString().replace("'","''");s.execute("VACUUM INTO '"+target+"'");
        }
        System.out.println("SQLite backup created: "+backup);
    }
}
