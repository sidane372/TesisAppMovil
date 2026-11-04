package com.example.santaceciliatesis.data.local.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public final class DatabaseMigrations {
    private DatabaseMigrations() {}

    public static final Migration MIGRATION_2_3 =
            new Migration(2, 3) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase db) {
                    db.execSQL(
                            "ALTER TABLE servicio_cache ADD COLUMN generosJson TEXT NOT NULL"
                                    + " DEFAULT '[]'");
                    db.execSQL(
                            "ALTER TABLE servicio_cache ADD COLUMN instrumentosJson TEXT NOT NULL"
                                    + " DEFAULT '[]'");
                }
            };
    public static final Migration MIGRATION_1_2 =
            new Migration(1, 2) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase db) {
                    db.execSQL(
                            "CREATE TABLE IF NOT EXISTS cache_scope (nombre TEXT NOT NULL, origen"
                                    + " TEXT NOT NULL, sincronizadoEn INTEGER NOT NULL, PRIMARY"
                                    + " KEY(nombre))");
                }
            };
}
