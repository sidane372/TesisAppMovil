package com.example.santaceciliatesis.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.santaceciliatesis.data.local.entity.CacheScopeEntity;
import com.example.santaceciliatesis.data.local.entity.ServicioCacheEntity;

import java.util.List;

@Dao
public abstract class ServicioDao {
    @Query("SELECT * FROM servicio_cache ORDER BY nombre COLLATE NOCASE, id")
    public abstract List<ServicioCacheEntity> obtenerTodos();

    @Query("DELETE FROM servicio_cache")
    protected abstract void eliminarTodos();

    @Insert
    protected abstract void insertar(List<ServicioCacheEntity> servicios);

    @Query("SELECT origen FROM cache_scope WHERE nombre='catalogo'")
    protected abstract String origenActual();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void guardarScope(CacheScopeEntity scope);

    @Transaction
    public List<ServicioCacheEntity> leerCatalogo(String origen) {
        if (!origen.equals(origenActual())) {
            eliminarTodos();
            CacheScopeEntity scope = new CacheScopeEntity();
            scope.origen = origen;
            guardarScope(scope);
        }
        return obtenerTodos();
    }

    @Transaction
    public void guardarCatalogo(String origen, List<ServicioCacheEntity> datos, long fecha) {
        if (!origen.equals(origenActual()))
            throw new IllegalStateException("Origen de cache no coincide");
        eliminarTodos();
        insertar(datos);
        CacheScopeEntity scope = new CacheScopeEntity();
        scope.origen = origen;
        scope.sincronizadoEn = fecha;
        guardarScope(scope);
    }

    /** Solo para respuestas completas del catálogo público, nunca para una página parcial. */
    @Transaction
    public void reemplazarCatalogo(List<ServicioCacheEntity> servicios) {
        eliminarTodos();
        insertar(servicios);
    }
}
