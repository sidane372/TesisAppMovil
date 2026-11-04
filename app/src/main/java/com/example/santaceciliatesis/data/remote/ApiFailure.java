package com.example.santaceciliatesis.data.remote;

import java.util.Collections;
import java.util.Map;

public final class ApiFailure extends Exception {
    public enum Kind {
        NOT_CONFIGURED,
        LOCAL_NETWORK_PERMISSION,
        NETWORK,
        TLS,
        HTTP,
        INVALID_DATA,
        STORAGE,
        AUTH_REQUIRED,
        SESSION_CHANGED,
        CANCELLED
    }

    private final Kind kind;
    private final int status;
    private final Map<String, String> fields;
    private final boolean sent;

    public ApiFailure(Kind kind, String message) {
        this(kind, 0, message, Collections.emptyMap(), false);
    }

    public ApiFailure(
            Kind kind, int status, String message, Map<String, String> fields, boolean sent) {
        super(message);
        this.kind = kind;
        this.status = status;
        this.fields = Collections.unmodifiableMap(fields);
        this.sent = sent;
    }

    public Kind getKind() {
        return kind;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public boolean mayHaveBeenSent() {
        return sent;
    }

    public static ApiFailure session() {
        return new ApiFailure(Kind.AUTH_REQUIRED, "Tu sesión venció. Inicia sesión nuevamente.");
    }
}
