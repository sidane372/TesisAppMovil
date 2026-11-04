package com.example.santaceciliatesis.data.remote;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.santaceciliatesis.data.remote.mapper.ServicioJsonMapper;
import org.json.JSONObject;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class ServicioJsonMapperTest {
    private String service(String price) { return "{\"id\":1,\"nombre\":\"Prueba\",\"tipo\":\"BANDA\",\"ciudad\":\"Ambo\",\"tarifaCentimos\":"+price+"}"; }
    @Test public void acceptsNullablePrice() throws Exception {
        assertNull(ServicioJsonMapper.parse(new JSONObject("{\"completo\":true,\"data\":["+service("null")+"]}")).get(0).getTarifaCentimos());
    }
    @Test(expected=JSONException.class) public void rejectsPartialSnapshot() throws Exception {
        ServicioJsonMapper.parse(new JSONObject("{\"completo\":false,\"data\":[]}"));
    }
    @Test(expected=JSONException.class) public void rejectsFractionalCent() throws Exception {
        ServicioJsonMapper.parse(new JSONObject("{\"completo\":true,\"data\":["+service("1.5")+"]}"));
    }
    @Test(expected=JSONException.class) public void rejectsDuplicateIds() throws Exception {
        ServicioJsonMapper.parse(new JSONObject("{\"completo\":true,\"data\":["+service("10")+","+service("10")+"]}"));
    }
}
