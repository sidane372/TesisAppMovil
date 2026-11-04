package com.example.santaceciliatesis.data.repository;

import com.example.santaceciliatesis.data.remote.ApiFailure;

public interface Callback<T> {
    void onSuccess(T data);

    void onFailure(ApiFailure error);
}
