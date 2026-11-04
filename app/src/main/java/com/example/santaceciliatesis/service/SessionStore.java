package com.example.santaceciliatesis.service;

import android.content.Context;
import android.security.keystore.*;
import android.util.AtomicFile;

import com.example.santaceciliatesis.data.remote.mapper.AuthJsonMapper;
import com.example.santaceciliatesis.model.Sesion;

import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-256-GCM, clave no exportable en Keystore y fichero privado excluido de backup. Solo en IO.
 */
public final class SessionStore {
    private static final String ALIAS = "santa_cecilia_sesion_v1";
    private final AtomicFile file;
    private final byte[] aad;

    public SessionStore(Context context, String origin) {
        file = new AtomicFile(new File(context.getNoBackupFilesDir(), "sesion.enc"));
        aad = (context.getPackageName() + "|" + origin).getBytes(StandardCharsets.UTF_8);
    }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(
                new KeyGenParameterSpec.Builder(
                                ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setKeySize(256)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .build());
        return generator.generateKey();
    }

    public Sesion read() throws Exception {
        if (!file.getBaseFile().exists()) return null;
        byte[] bytes = file.readFully();
        if (bytes.length < 30 || bytes.length > 65536 || bytes[0] != 1)
            throw new IOException("Formato de sesión inválido");
        byte[] iv = java.util.Arrays.copyOfRange(bytes, 1, 13);
        byte[] encrypted = java.util.Arrays.copyOfRange(bytes, 13, bytes.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        cipher.updateAAD(aad);
        return AuthJsonMapper.restaurar(
                new JSONObject(new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)));
    }

    public void save(Sesion session) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        cipher.updateAAD(aad);
        byte[] encrypted =
                cipher.doFinal(
                        AuthJsonMapper.guardar(session)
                                .toString()
                                .getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        if (iv.length != 12) throw new IOException("IV no válido");
        ByteArrayOutputStream envelope = new ByteArrayOutputStream();
        envelope.write(1);
        envelope.write(iv);
        envelope.write(encrypted);
        byte[] expected = envelope.toByteArray();
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            output.write(expected);
            file.finishWrite(output);
            output = null;
            if (!java.security.MessageDigest.isEqual(expected, file.readFully()))
                throw new IOException("No se confirmó la escritura de sesión");
        } catch (Exception error) {
            if (output != null) file.failWrite(output);
            throw error;
        }
    }

    public void clear() throws Exception {
        file.delete();
        // Si falla borrar el archivo, invalidar la clave impide restaurar sus credenciales.
        if (file.getBaseFile().exists()) {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            store.deleteEntry(ALIAS);
            if (store.containsAlias(ALIAS))
                throw new IOException("No se pudo cerrar la sesión local");
        }
    }
}
