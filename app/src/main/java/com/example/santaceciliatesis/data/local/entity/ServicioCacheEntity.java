package com.example.santaceciliatesis.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "servicio_cache")
public final class ServicioCacheEntity {
    @PrimaryKey public long id;
    @NonNull public String nombre = "";
    @NonNull public String tipo = "";
    @NonNull public String ciudad = "";
    public Long tarifaCentimos;

    @NonNull
    @androidx.room.ColumnInfo(defaultValue = "'[]'")
    public String generosJson = "[]";

    @NonNull
    @androidx.room.ColumnInfo(defaultValue = "'[]'")
    public String instrumentosJson = "[]";

    public long sincronizadoEn;
}
