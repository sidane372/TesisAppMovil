package com.santacecilia.api.security;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import org.springframework.stereotype.Component;
@Component
public class TokenCodec {
    private final SecureRandom random=new SecureRandom();
    public String nuevo(String prefix){byte[] bytes=new byte[32];random.nextBytes(bytes);return prefix+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    public byte[] hash(String raw){
        try{return MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 no disponible",e);}
    }
    public boolean formato(String raw,String prefix){return raw!=null&&raw.matches(prefix+"\\.[A-Za-z0-9_-]{43}");}
}
