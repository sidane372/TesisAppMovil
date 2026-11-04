package com.example.santaceciliatesis.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity(tableName = "cache_scope")
public final class CacheScopeEntity {
    @PrimaryKey @NonNull public String nombre = "catalogo";
    @NonNull public String origen = "";
    public long sincronizadoEn;
}
