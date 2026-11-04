package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.widget.*;

import org.json.*;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

public final class AgendaFragment extends FlujoBaseFragment {
    private YearMonth month = YearMonth.now(PERU);
    private LocalDate selected = LocalDate.now(PERU);
    private JSONObject agenda;
    private LinearLayout dayPanel;
    private boolean privada;

    public static AgendaFragment nuevo(long id, boolean privada) {
        var f = new AgendaFragment();
        Bundle b = new Bundle();
        b.putLong("id", id);
        b.putBoolean("privada", privada);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(android.view.View view, Bundle saved) {
        super.onViewCreated(view, saved);
        privada = requireArguments().getBoolean("privada");
        if (saved != null) {
            month = YearMonth.parse(saved.getString("month", month.toString()));
            selected = LocalDate.parse(saved.getString("selected", selected.toString()));
        }
    }

    @Override
    public void refresh() {
        title(privada ? "Calendario de trabajo" : "Disponibilidad");
        long id = requireArguments().getLong("id");
        if (id == 0) {
            clear();
            boolean admin = role() == com.example.santaceciliatesis.model.Rol.ADMINISTRADOR;
            request(
                    "GET",
                    admin ? "admin/servicios?pagina=0&tamano=100" : "mis-servicios",
                    null,
                    false,
                    d -> {
                        JSONArray rows = d.optJSONArray("items");
                        if (rows == null) rows = d.optJSONArray("servicios");
                        if (rows == null)
                            rows = new JSONArray(); // The repository normalizes list responses.
                        if (rows.length() == 0)
                            label(
                                    card(),
                                    "No hay servicios disponibles para configurar. Abre un servicio"
                                        + " desde su administración.",
                                    false);
                        for (int i = 0; i < rows.length(); i++) {
                            JSONObject s = rows.optJSONObject(i);
                            button(
                                    card(),
                                    s.optString("nombre"),
                                    () -> host().flujo("agenda", s.optLong("id")));
                        }
                    });
            return;
        }
        request(
                "GET",
                "servicios/"
                        + id
                        + (privada ? "/agenda" : "/disponibilidad")
                        + "?desde="
                        + month.atDay(1)
                        + "&hasta="
                        + month.atEndOfMonth(),
                null,
                !privada,
                d -> {
                    agenda = d;
                    render();
                });
    }

    private void render() {
        clear();
        var header = card();
        label(
                header,
                month.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "PE"))
                        + " "
                        + month.getYear(),
                true);
        button(
                header,
                "‹ Mes anterior",
                () -> {
                    month = month.minusMonths(1);
                    refresh();
                });
        button(
                header,
                "Mes siguiente ›",
                () -> {
                    month = month.plusMonths(1);
                    refresh();
                });
        GridLayout grid = new GridLayout(requireContext());
        grid.setColumnCount(7);
        grid.setRowCount(7);
        add(content, grid);
        for (String day : new String[] {"L", "M", "M", "J", "V", "S", "D"})
            cell(grid, day, null, 0, false);
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) cell(grid, "", null, 0, false);
        for (int n = 1; n <= month.lengthOfMonth(); n++) {
            LocalDate day = month.atDay(n);
            int state = classification(day);
            cell(grid, String.valueOf(n), day, state, state != 0 && state != 4);
        }
        label(
                content,
                "Verde: disponible · Amarillo: reservado por pago · Rosa: ocupado · Gris: no"
                    + " habilitado. Toca un día para consultar las horas.",
                false);
        label(
                content,
                "El pago aprobado reserva automáticamente el horario. Los demás horarios del día se"
                    + " validan al solicitar y pagar. Los rangos ocupados pueden incluir"
                    + " preparación.",
                false);
        var hours = card();
        label(hours, "Horario de atención · Perú", true);
        JSONArray franjas = agenda.optJSONArray("horarios");
        if (franjas == null || franjas.length() == 0)
            label(
                    hours,
                    "Sin atención configurada. El proveedor debe habilitar sus horarios antes de"
                        + " recibir solicitudes.",
                    false);
        else
            for (int i = 0; i < franjas.length(); i++) {
                var f = franjas.optJSONObject(i);
                label(
                        hours,
                        DayOfWeek.of(f.optInt("dia_semana"))
                                        .getDisplayName(TextStyle.FULL, new Locale("es", "PE"))
                                + " "
                                + f.optString("hora_inicio")
                                + "–"
                                + f.optString("hora_fin")
                                + " · Preparación "
                                + f.optInt("margen_previo_min")
                                + " min antes / "
                                + f.optInt("margen_posterior_min")
                                + " min después",
                        false);
            }
        if (privada)
            button(
                    hours,
                    "Configurar franjas de atención",
                    () -> host().flujo("horarios", requireArguments().getLong("id")));
        dayPanel = card();
        renderDay();
        if (privada) {
            var box = card();
            label(box, "Bloquear un intervalo", true);
            var start = dateField(box, "Inicio del bloqueo", selected.atTime(10, 0).format(LOCAL));
            var end = dateField(box, "Fin del bloqueo", selected.atTime(13, 0).format(LOCAL));
            var reason = field(box, "Motivo privado", "", 1000, 131073);
            button(
                    box,
                    "Guardar bloqueo",
                    () -> {
                        try {
                            request(
                                    "POST",
                                    "servicios/" + requireArguments().getLong("id") + "/bloqueos",
                                    obj(
                                            "inicio",
                                            utc(value(start)),
                                            "fin",
                                            utc(value(end)),
                                            "motivo",
                                            value(reason)),
                                    false,
                                    d -> refresh());
                        } catch (Exception e) {
                            invalid("Revisa el intervalo del bloqueo");
                        }
                    });
            JSONArray blocks = agenda.optJSONArray("bloqueos");
            if (blocks != null && blocks.length() > 0) {
                label(content, "Bloqueos propios", true);
                for (int i = 0; i < blocks.length(); i++) {
                    JSONObject b = blocks.optJSONObject(i);
                    var c = card();
                    label(
                            c,
                            date(b.optString("inicio_utc"))
                                    + " → "
                                    + date(b.optString("fin_utc"))
                                    + "\n"
                                    + b.optString("motivo"),
                            false);
                    button(
                            c,
                            "Retirar bloqueo",
                            () ->
                                    new com.google.android.material.dialog
                                                    .MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("Retirar bloqueo")
                                            .setMessage(
                                                    "Se liberará este intervalo en los recursos"
                                                        + " originales del bloqueo.")
                                            .setNegativeButton("Volver", null)
                                            .setPositiveButton(
                                                    "Retirar",
                                                    (dialog, w) ->
                                                            request(
                                                                    "DELETE",
                                                                    "servicios/"
                                                                            + requireArguments()
                                                                                    .getLong("id")
                                                                            + "/bloqueos/"
                                                                            + b.optLong("id"),
                                                                    null,
                                                                    false,
                                                                    d -> refresh()))
                                            .show());
                }
            }
        }
    }

    private int classification(LocalDate day) {
        if (day.isBefore(Instant.parse(agenda.optString("ahora")).atZone(PERU).toLocalDate()))
            return 0;
        boolean enabled = false;
        JSONArray hours = agenda.optJSONArray("horarios");
        if (hours != null)
            for (int i = 0; i < hours.length(); i++)
                if (hours.optJSONObject(i).optInt("dia_semana") == day.getDayOfWeek().getValue())
                    enabled = true;
        if (!enabled) return 0;
        boolean occupied = false, reserved = false, full = false;
        Instant from = day.atStartOfDay(PERU).toInstant(),
                to = day.plusDays(1).atStartOfDay(PERU).toInstant();
        JSONArray spans = agenda.optJSONArray("ocupaciones");
        if (spans != null)
            for (int i = 0; i < spans.length(); i++) {
                var r = spans.optJSONObject(i);
                Instant start = Instant.parse(r.optString("inicio_utc")),
                        end = Instant.parse(r.optString("fin_utc"));
                if (start.isBefore(to) && end.isAfter(from)) {
                    occupied = true;
                    reserved |= "RESERVADO".equals(r.optString("estado"));
                    full |= !start.isAfter(from) && !end.isBefore(to);
                }
            }
        return reserved ? 2 : full ? 4 : occupied ? 3 : 1;
    }

    private void cell(GridLayout grid, String text, LocalDate day, int state, boolean enabled) {
        TextView t = new TextView(requireContext());
        t.setText(text);
        t.setTextSize(14);
        t.setGravity(android.view.Gravity.CENTER);
        t.setMinHeight(dp(48));
        t.setMinWidth(0);
        var p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = -2;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(1), dp(1), dp(1), dp(1));
        t.setLayoutParams(p);
        int color =
                state == 1
                        ? 0xFFECF7EE
                        : state == 2 ? 0xFFFFE7A3 : state >= 3 ? 0xFFFAEAE7 : 0xFFF2F4F7;
        var bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(5));
        if (day != null && day.equals(selected)) bg.setStroke(dp(2), 0xFF3158A8);
        t.setBackground(bg);
        t.setTextColor(state == 0 ? 0xFF687789 : 0xFF14253D);
        if (day != null) {
            t.setContentDescription(
                    day
                            + " · "
                            + new String[] {
                                        "No habilitado",
                                        "Disponible",
                                        "Reservado parcialmente",
                                        "Ocupado parcialmente",
                                        "Ocupado completo"
                                    }
                                    [state]);
            t.setEnabled(enabled);
            t.setOnClickListener(
                    v -> {
                        if (!busy) {
                            selected = day;
                            render();
                        }
                    });
        }
        grid.addView(t);
    }

    private void renderDay() {
        dayPanel.removeAllViews();
        label(dayPanel, "Intervalos del " + selected, true);
        int state = classification(selected);
        if (state == 0)
            label(dayPanel, "Este día no está habilitado para nuevas solicitudes.", false);
        JSONArray spans = agenda.optJSONArray("ocupaciones");
        boolean any = false;
        Instant start = selected.atStartOfDay(PERU).toInstant(),
                end = selected.plusDays(1).atStartOfDay(PERU).toInstant();
        if (spans != null)
            for (int i = 0; i < spans.length(); i++) {
                var r = spans.optJSONObject(i);
                if (Instant.parse(r.optString("inicio_utc")).isBefore(end)
                        && Instant.parse(r.optString("fin_utc")).isAfter(start)) {
                    any = true;
                    label(
                            dayPanel,
                            r.optString("estado")
                                    + "\n"
                                    + date(r.optString("inicio_utc"))
                                    + " → "
                                    + date(r.optString("fin_utc")),
                            false);
                }
            }
        if (!any) label(dayPanel, "Sin ocupaciones registradas para este día.", false);
        if (!privada && state != 0 && state != 4)
            button(
                    dayPanel,
                    "Solicitar cotización para este día",
                    () -> {
                        if (role() == com.example.santaceciliatesis.model.Rol.CLIENTE)
                            host().solicitar(requireArguments().getLong("id"), selected.toString());
                        else if (role() == null) host().mostrarCuenta();
                        else invalid("Las solicitudes se envían desde una cuenta de cliente");
                    });
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putString("month", month.toString());
        out.putString("selected", selected.toString());
        super.onSaveInstanceState(out);
    }
}
