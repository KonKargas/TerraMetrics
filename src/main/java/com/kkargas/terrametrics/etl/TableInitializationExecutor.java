package com.kkargas.terrametrics.etl;

import java.sql.*;

/** SQLite DDL; called by CsvToDBImporter inside its single transaction. */
public final class TableInitializationExecutor {
    private TableInitializationExecutor() {}
    public static void main(String[] args)throws Exception {CsvToDBImporter.main(args);}
    static void createTables(Connection connection)throws SQLException {
        String[] ddl={
            "CREATE TABLE geographic_entities (EntityID INTEGER PRIMARY KEY, ISO3 TEXT NOT NULL UNIQUE, ISO2 TEXT, Name TEXT NOT NULL, EntityType TEXT NOT NULL CHECK(EntityType IN ('COUNTRY','AGGREGATE_OR_UNMATCHED')))",
            "CREATE TABLE countries (EntityID INTEGER PRIMARY KEY REFERENCES geographic_entities(EntityID), ISO TEXT, ISO3 TEXT NOT NULL, ISO_Code INTEGER, FIPS TEXT, Display_Name TEXT, Official_Name TEXT, Capital TEXT, Continent TEXT, CurrencyCode TEXT, CurrencyName TEXT, Phone TEXT, \"Region Code\" INTEGER, \"Region Name\" TEXT, \"Sub-region Code\" INTEGER, \"Sub-region Name\" TEXT, \"Intermediate Region Code\" INTEGER, \"Intermediate Region Name\" TEXT, Status TEXT, \"Developed or Developing\" TEXT, \"Small Island Developing States (SIDS)\" TEXT, \"Land Locked Developing Countries (LLDC)\" TEXT, \"Least Developed Countries (LDC)\" TEXT, Area_SqKm REAL, Population INTEGER)",
            "CREATE TABLE units (UnitID INTEGER PRIMARY KEY, Value TEXT NOT NULL UNIQUE)",
            "CREATE TABLE ctss (CTS_ID INTEGER PRIMARY KEY, CTS_Code TEXT, CTS_Name TEXT, CTS_Full_Descriptor TEXT)",
            "CREATE TABLE sources (SourceID INTEGER PRIMARY KEY, Value TEXT NOT NULL UNIQUE)",
            "CREATE TABLE climate_influences (InfluenceID INTEGER PRIMARY KEY, Value TEXT NOT NULL UNIQUE)",
            "CREATE TABLE indicators (IndicatorID INTEGER PRIMARY KEY, Dataset TEXT NOT NULL, Value TEXT NOT NULL, UnitID INTEGER NOT NULL REFERENCES units(UnitID), CTS_ID INTEGER NOT NULL REFERENCES ctss(CTS_ID), UNIQUE(Dataset,Value,UnitID,CTS_ID))",
            "CREATE TABLE climate_records (RecordID INTEGER PRIMARY KEY, EntityID INTEGER NOT NULL REFERENCES geographic_entities(EntityID), IndicatorID INTEGER NOT NULL REFERENCES indicators(IndicatorID), SourceID INTEGER NOT NULL REFERENCES sources(SourceID), InfluenceID INTEGER REFERENCES climate_influences(InfluenceID), Year INTEGER NOT NULL CHECK(Year BETWEEN 1800 AND 2200), Value REAL NOT NULL)",
            "CREATE UNIQUE INDEX uq_record_measure ON climate_records(EntityID,IndicatorID,SourceID,COALESCE(InfluenceID,0),Year)",
            "CREATE INDEX idx_record_indicator_year ON climate_records(IndicatorID,Year)",
            "CREATE INDEX idx_record_entity_year ON climate_records(EntityID,Year)"
        };
        try(Statement s=connection.createStatement()){for(String statement:ddl)s.execute(statement);}
    }
}
