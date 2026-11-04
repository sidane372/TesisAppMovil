package com.example.santaceciliatesis.util;

/** Resultado de consulta; los errores técnicos no se imprimen en la interfaz. */
public final class Resource<T> {
    public enum State {
        LOADING,
        REMOTE,
        CACHE,
        NOT_CONFIGURED,
        LOCAL_PERMISSION,
        NETWORK_ERROR,
        INVALID_DATA,
        LOCAL_ERROR
    }

    private final State state;
    private final T data;

    public Resource(State state, T data) {
        this.state = state;
        this.data = data;
    }

    public State getState() {
        return state;
    }

    public T getData() {
        return data;
    }
}
