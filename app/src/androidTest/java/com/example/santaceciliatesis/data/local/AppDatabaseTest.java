package com.example.santaceciliatesis.data.local;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.santaceciliatesis.data.local.entity.ServicioCacheEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class AppDatabaseTest {
    private AppDatabase database;
    @Before public void create() { database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase.class).build(); }
    @After public void close() { database.close(); }
    private ServicioCacheEntity servicio(long id) {
        ServicioCacheEntity e=new ServicioCacheEntity(); e.id=id; e.nombre="Servicio de prueba"; e.tipo="BANDA";
        e.ciudad="Ambo"; e.tarifaCentimos=12345L; e.sincronizadoEn=1; return e;
    }
    @Test public void storesExactPriceAndRemovesWithdrawnServices() {
        database.servicioDao().reemplazarCatalogo(Collections.singletonList(servicio(1)));
        assertEquals(Long.valueOf(12345), database.servicioDao().obtenerTodos().get(0).tarifaCentimos);
        database.servicioDao().reemplazarCatalogo(Collections.emptyList());
        assertTrue(database.servicioDao().obtenerTodos().isEmpty());
    }
    @Test public void invalidReplacementRollsBackEntireSnapshot() {
        database.servicioDao().reemplazarCatalogo(Collections.singletonList(servicio(1)));
        try {
            database.servicioDao().reemplazarCatalogo(Arrays.asList(servicio(2),servicio(2))); fail("Debe rechazar IDs duplicados");
        } catch (android.database.sqlite.SQLiteConstraintException expected) {
            assertEquals(1,database.servicioDao().obtenerTodos().size());
            assertEquals(1,database.servicioDao().obtenerTodos().get(0).id);
        }
    }
}
