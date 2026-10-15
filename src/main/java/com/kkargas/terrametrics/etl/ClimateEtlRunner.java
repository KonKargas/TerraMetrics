package com.kkargas.terrametrics.etl;

/** Single command for: transform -> SQLite import -> backup. */
public final class ClimateEtlRunner {
    public static void main(String[] args) throws Exception {
        ClimateEtl.transform();
        CsvToDBImporter.importCsv();
        DBBackupExtractor.main(args);
    }
}