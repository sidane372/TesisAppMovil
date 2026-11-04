package com.example.santaceciliatesis.util;

import android.content.Context;
import android.widget.TextView;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.model.*;

public final class Presentacion {
    private Presentacion() {}

    public static String rol(Rol role) {
        switch (role) {
            case ADMINISTRADOR:
                return "Administrador";
            case DIRECTOR:
                return "Director de banda";
            case SOLISTA:
                return "Solista";
            default:
                return "Cliente";
        }
    }

    public static String estado(String value) {
        switch (value) {
            case "BORRADOR":
                return "Borrador";
            case "EN_REVISION":
                return "Por verificar";
            case "PUBLICADO":
                return "Publicado";
            case "OBSERVADO":
                return "Observado";
            case "RETIRADO":
                return "Desactivado";
            case "ACTIVO":
                return "Activo";
            case "SUSPENDIDO":
                return "Suspendido";
            case "DESACTIVADO":
                return "Desactivado";
            default:
                return value;
        }
    }

    public static String tarifa(Context context, Long cents) {
        return cents == null
                ? context.getString(R.string.a_cotizar)
                : context.getString(R.string.tarifa_hora, MoneyUtils.formatPen(cents));
    }

    public static String disponible(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    public static void badge(TextView label, String state) {
        Context c = label.getContext();
        label.setText(estado(state));
        boolean success = "PUBLICADO".equals(state) || "ACTIVO".equals(state),
                error =
                        "RETIRADO".equals(state)
                                || "DESACTIVADO".equals(state)
                                || "OBSERVADO".equals(state);
        label.setBackgroundResource(
                success
                        ? R.drawable.bg_success
                        : error ? R.drawable.bg_notice : R.drawable.bg_badge);
        label.setTextColor(
                c.getColor(
                        success
                                ? R.color.sc_success_text
                                : error ? R.color.sc_warning_text : R.color.sc_blue));
    }
}
