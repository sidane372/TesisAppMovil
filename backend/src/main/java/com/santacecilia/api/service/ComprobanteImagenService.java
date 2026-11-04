package com.santacecilia.api.service;

import com.santacecilia.api.exception.ApiException;

import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

/** Solo JPEG/PNG, dimensiones acotadas y recodificación sin metadatos ni nombres del usuario. */
@Service
public class ComprobanteImagenService {
    private final java.util.concurrent.Semaphore slots = new java.util.concurrent.Semaphore(2);

    public byte[] normalizar(String base64) {
        if (!slots.tryAcquire())
            throw new ApiException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "IMAGEN_EN_PROCESO",
                    "Hay comprobantes procesándose. Intenta nuevamente en unos segundos");
        try {
            return decodificar(base64);
        } finally {
            slots.release();
        }
    }

    private byte[] decodificar(String base64) {
        try {
            byte[] raw = Base64.getDecoder().decode(base64);
            if (raw.length == 0 || raw.length > 2097152)
                throw ApiException.invalid("El comprobante debe pesar hasta 2 MB");
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(raw))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw ApiException.invalid("Adjunta una imagen JPEG o PNG");
                var reader = readers.next();
                try {
                    String format = reader.getFormatName();
                    if (!Set.of("JPEG", "PNG").contains(format.toUpperCase(Locale.ROOT)))
                        throw ApiException.invalid("Adjunta una imagen JPEG o PNG");
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1
                            || height < 1
                            || width > 4096
                            || height > 4096
                            || (long) width * height > 4000000)
                        throw ApiException.invalid(
                                "El comprobante debe tener hasta cuatro millones de píxeles");
                    BufferedImage original = reader.read(0);
                    BufferedImage safe =
                            new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    var graphics = safe.createGraphics();
                    try {
                        graphics.setColor(java.awt.Color.WHITE);
                        graphics.fillRect(0, 0, width, height);
                        graphics.drawImage(original, 0, 0, null);
                    } finally {
                        graphics.dispose();
                    }
                    var output = new ByteArrayOutputStream();
                    ImageIO.write(safe, "jpeg", output);
                    byte[] result = output.toByteArray();
                    if (result.length > 1048576)
                        throw ApiException.invalid("Reduce la imagen a un máximo de 1 MB");
                    return result;
                } finally {
                    reader.dispose();
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.invalid("No se pudo leer el comprobante JPEG o PNG");
        }
    }
}
