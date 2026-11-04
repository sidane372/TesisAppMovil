package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.core.widget.TextViewCompat;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.controller.FlujoController;
import com.example.santaceciliatesis.data.remote.ApiFailure;
import com.example.santaceciliatesis.model.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.*;

import org.json.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Controles nativos compartidos para agregados y formularios de longitud variable. */
public abstract class FlujoBaseFragment extends BaseFragment
        implements FlujoController.View, Refreshable {
    protected FlujoController controller;
    protected LinearLayout content;
    protected boolean busy;
    private final Map<MaterialButton, MaterialCheckBox> consents = new IdentityHashMap<>();
    protected static final ZoneId PERU = ZoneId.of("America/Lima");
    protected static final DateTimeFormatter LOCAL =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    protected FlujoBaseFragment() {
        super(R.layout.fragment_flujo);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        content = v(R.id.flujo_contenido);
        controller = new FlujoController(deps().getContratacionRepository());
    }

    @Override
    public void onStart() {
        super.onStart();
        controller.attach(this);
        refresh();
    }

    @Override
    public void onStop() {
        controller.detach();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        consents.clear();
        content = null;
        super.onDestroyView();
    }

    protected void title(String title) {
        host().titulo(title);
        text(R.id.flujo_titulo, title);
    }

    protected void clear() {
        consents.clear();
        content.removeAllViews();
    }

    protected int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    protected void add(LinearLayout target, View view) {
        var p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(10);
        view.setLayoutParams(p);
        target.addView(view);
    }

    protected LinearLayout card() {
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(16));
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(requireContext().getColor(R.color.sc_white));
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), requireContext().getColor(R.color.sc_border));
        box.setBackground(bg);
        add(content, box);
        return box;
    }

    protected TextView label(LinearLayout target, String text, boolean heading) {
        TextView t = new TextView(requireContext());
        TextViewCompat.setTextAppearance(t, heading ? R.style.SC_Heading : R.style.SC_Body);
        t.setText(text);
        if (heading) t.setAccessibilityHeading(true);
        t.setTextIsSelectable(true);
        add(target, t);
        return t;
    }

    protected MaterialButton button(LinearLayout target, String text, Runnable action) {
        MaterialButton b = new MaterialButton(requireContext());
        b.setText(text);
        b.setAllCaps(false);
        b.setMinHeight(dp(48));
        b.setMaxLines(4);
        b.setCornerRadius(dp(7));
        b.setOnClickListener(
                v -> {
                    if (!busy) action.run();
                });
        add(target, b);
        return b;
    }

    protected void requireConsent(MaterialButton action, MaterialCheckBox consent) {
        consents.put(action, consent);
        action.setEnabled(!busy && consent.isChecked());
        consent.setOnCheckedChangeListener((view, checked) -> action.setEnabled(!busy && checked));
    }

    protected TextInputEditText field(
            LinearLayout target, String label, String value, int max, int input) {
        TextInputLayout box = new TextInputLayout(requireContext());
        box.setHint(label);
        box.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText edit = new TextInputEditText(box.getContext());
        edit.setInputType(input);
        if ((input & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0) {
            edit.setMinLines(2);
            edit.setMaxLines(6);
        } else edit.setSingleLine(true);
        edit.setFilters(
                new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(max)});
        edit.setText(value);
        edit.setSaveEnabled(false);
        edit.setId(View.generateViewId());
        box.addView(edit, new LinearLayout.LayoutParams(-1, -2));
        add(target, box);
        return edit;
    }

    protected MaterialCheckBox check(LinearLayout target, String label, boolean checked) {
        MaterialCheckBox c = new MaterialCheckBox(requireContext());
        c.setText(label);
        c.setChecked(checked);
        c.setSaveEnabled(false);
        add(target, c);
        return c;
    }

    protected TextInputEditText dateField(LinearLayout target, String label, String value) {
        TextInputEditText e = field(target, label, value, 16, InputType.TYPE_CLASS_DATETIME);
        e.setFocusable(false);
        e.setOnClickListener(
                v -> {
                    LocalDateTime initial;
                    try {
                        initial = LocalDateTime.parse(e.getText().toString(), LOCAL);
                    } catch (Exception x) {
                        initial = LocalDateTime.now(PERU).plusDays(1).withHour(10).withMinute(0);
                    }
                    final LocalDateTime chosen = initial;
                    new android.app.DatePickerDialog(
                                    requireContext(),
                                    (picker, y, m, d) ->
                                            new android.app.TimePickerDialog(
                                                            requireContext(),
                                                            (time, h, min) ->
                                                                    e.setText(
                                                                            LocalDateTime.of(
                                                                                            y,
                                                                                            m + 1,
                                                                                            d, h,
                                                                                            min)
                                                                                    .format(LOCAL)),
                                                            chosen.getHour(),
                                                            chosen.getMinute(),
                                                            true)
                                                    .show(),
                                    chosen.getYear(),
                                    chosen.getMonthValue() - 1,
                                    chosen.getDayOfMonth())
                            .show();
                });
        return e;
    }

    protected TextInputEditText dayField(LinearLayout target, String label, String value) {
        TextInputEditText edit =
                field(target, label, value, 10, android.text.InputType.TYPE_CLASS_DATETIME);
        edit.setFocusable(false);
        edit.setOnClickListener(
                v -> {
                    java.time.LocalDate date;
                    try {
                        date = java.time.LocalDate.parse(edit.getText().toString());
                    } catch (Exception e) {
                        date = java.time.LocalDate.now(PERU);
                    }
                    new android.app.DatePickerDialog(
                                    requireContext(),
                                    (picker, y, m, d) ->
                                            edit.setText(
                                                    java.time.LocalDate.of(y, m + 1, d).toString()),
                                    date.getYear(),
                                    date.getMonthValue() - 1,
                                    date.getDayOfMonth())
                            .show();
                });
        return edit;
    }

    protected String value(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }

    protected static String date(String iso) {
        try {
            return Instant.parse(iso)
                    .atZone(PERU)
                    .format(
                            DateTimeFormatter.ofPattern(
                                    "dd MMM yyyy · HH:mm", new Locale("es", "PE")));
        } catch (Exception e) {
            return iso;
        }
    }

    protected static String local(String iso) {
        return Instant.parse(iso).atZone(PERU).format(LOCAL);
    }

    protected static String utc(String local) {
        return LocalDateTime.parse(local, LOCAL).atZone(PERU).toInstant().toString();
    }

    protected static String money(long cents) {
        return String.format(new Locale("es", "PE"), "S/ %,.2f", cents / 100.0);
    }

    protected static long cents(String value) {
        return new java.math.BigDecimal(value.replace(',', '.')).movePointRight(2).longValueExact();
    }

    protected static String decimal(long cents) {
        return java.math.BigDecimal.valueOf(cents, 2).toPlainString();
    }

    protected static JSONObject obj(Object... pairs) {
        JSONObject o = new JSONObject();
        try {
            for (int i = 0; i < pairs.length; i += 2) o.put((String) pairs[i], pairs[i + 1]);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        return o;
    }

    protected void request(
            String method,
            String path,
            JSONObject body,
            boolean publica,
            FlujoController.Result result) {
        if (host().solicitarRedLocal()) controller.ejecutar(method, path, body, publica, result);
    }

    protected Rol role() {
        var s = deps().getSessionManager().snapshot().session;
        return s == null ? null : s.getUsuario().getRol();
    }

    protected long actor() {
        var s = deps().getSessionManager().snapshot().session;
        return s == null ? 0 : s.getUsuario().getId();
    }

    public void ocupado(boolean value) {
        busy = value;
        visible(R.id.flujo_progress, value);
        text(
                R.id.flujo_estado,
                value ? "Conectando…" : "Hora de Perú · Información consultada en el servidor");
        enableTree(content, !value);
        consents.forEach((action, consent) -> action.setEnabled(!value && consent.isChecked()));
    }

    private void enableTree(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup)
            for (int n = 0; n < ((ViewGroup) view).getChildCount(); n++)
                enableTree(((ViewGroup) view).getChildAt(n), enabled);
    }

    public void error(ApiFailure e) {
        text(
                R.id.flujo_estado,
                e.getMessage()
                        + (e.mayHaveBeenSent()
                                ? " Si la respuesta se perdió, consulta el detalle antes de volver"
                                        + " a enviar."
                                : ""));
    }

    protected void invalid(String text) {
        text(R.id.flujo_estado, text);
        host().mostrarMensaje(text);
    }

    protected void reason(String title, java.util.function.Consumer<String> action) {
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("Motivo (mínimo 5 caracteres)");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setFilters(
                new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(1000)});
        LinearLayout box = new LinearLayout(requireContext());
        box.setPadding(dp(20), dp(8), dp(20), dp(8));
        box.addView(input, new LinearLayout.LayoutParams(-1, -2));
        var dialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle(title)
                        .setView(box)
                        .setNegativeButton("Volver", null)
                        .setPositiveButton("Confirmar", null)
                        .create();
        dialog.setOnShowListener(
                v ->
                        dialog.getButton(-1)
                                .setOnClickListener(
                                        x -> {
                                            String reason = value(input);
                                            if (reason.length() < 5) {
                                                input.setError("Escribe al menos cinco caracteres");
                                                return;
                                            }
                                            dialog.dismiss();
                                            action.accept(reason);
                                        }));
        dialog.show();
    }

    protected void resetScroll() {
        v(R.id.flujo_scroll)
                .post(
                        () -> {
                            if (getView() != null)
                                ((androidx.core.widget.NestedScrollView) v(R.id.flujo_scroll))
                                        .smoothScrollTo(0, 0);
                        });
    }
}
