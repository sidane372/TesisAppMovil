package com.example.santaceciliatesis.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.santaceciliatesis.data.local.dao.ServicioDao;
import com.example.santaceciliatesis.data.local.entity.CacheScopeEntity;
import com.example.santaceciliatesis.data.local.entity.ServicioCacheEntity;
import com.example.santaceciliatesis.data.local.migration.DatabaseMigrations;

@Database(
        entities = {ServicioCacheEntity.class, CacheScopeEntity.class},
        version = 3,
        exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract ServicioDao servicioDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null)
                    instance =
                            Room.databaseBuilder(
                                            context.getApplicationContext(),
                                            AppDatabase.class,
                                            "santa_cecilia_cache.db")
                                    .addMigrations(
                                            DatabaseMigrations.MIGRATION_1_2,
                                            DatabaseMigrations.MIGRATION_2_3)
                                    .build();
            }
        }
        return instance;
    }
}
