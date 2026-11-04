package com.example.santaceciliatesis.data.remote;

import java.net.URI;

public final class ApiConfig {
    private final String baseUrl;

    public ApiConfig(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        if (!value.isEmpty()) {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                throw new IllegalArgumentException(
                        "La API debe usar HTTPS sin credenciales en la URL");
            }
            value = value.endsWith("/") ? value : value + "/";
        }
        this.baseUrl = value;
    }

    public boolean isLocalEndpoint() {
        if (!isConfigured()) return false;
        String host = URI.create(baseUrl).getHost().toLowerCase(java.util.Locale.ROOT);
        if (host.equals("localhost")
                || host.endsWith(".local")
                || host.equals("::1")
                || host.equals("[::1]")
                || host.startsWith("127.")
                || host.startsWith("10.")
                || host.startsWith("192.168.")) return true;
        String[] parts = host.split("\\.");
        if (parts.length == 4 && "172".equals(parts[0]))
            try {
                int second = Integer.parseInt(parts[1]);
                return second >= 16 && second <= 31;
            } catch (NumberFormatException ignored) {
            }
        return false;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isConfigured() {
        return !baseUrl.isEmpty();
    }

    public String endpoint(String relativePath) {
        if (!isConfigured()) throw new IllegalStateException("API no configurada");
        if (!ApiEndpoints.permitido(relativePath))
            throw new IllegalArgumentException("Endpoint no habilitado");
        return baseUrl + relativePath;
    }
}
