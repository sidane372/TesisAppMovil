package com.example.santaceciliatesis.view.fragment.common;

import android.content.Context;
import android.graphics.*;
import android.view.*;

import androidx.appcompat.widget.AppCompatImageView;

/** Visor privado con zoom/pan; no escribe comprobantes en almacenamiento compartido. */
final class ComprobanteDialog {
    static void mostrar(FlujoBaseFragment fragment, String base64) {
        try {
            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap == null) throw new IllegalArgumentException();
            Zoom image = new Zoom(fragment.requireContext());
            image.setImageBitmap(bitmap);
            image.setContentDescription(
                    "Comprobante de transferencia. Amplía con dos dedos y arrastra para"
                        + " desplazarte.");
            var dialog =
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                                    fragment.requireContext())
                            .setTitle("Comprobante · amplía con dos dedos")
                            .setView(image)
                            .setPositiveButton("Cerrar", null)
                            .create();
            dialog.setOnDismissListener(
                    d -> {
                        image.setImageDrawable(null);
                        bitmap.recycle();
                    });
            dialog.show();
            int height = (int) (fragment.getResources().getDisplayMetrics().heightPixels * .72f);
            image.getLayoutParams().height = height;
            image.requestLayout();
        } catch (Exception e) {
            fragment.invalid("No se pudo mostrar el comprobante");
        }
    }

    private static final class Zoom extends AppCompatImageView {
        private final Matrix matrix = new Matrix();
        private final ScaleGestureDetector scale;
        private float x, y;
        private boolean fitted;

        Zoom(Context context) {
            super(context);
            setScaleType(ScaleType.MATRIX);
            setMinimumHeight(600);
            scale =
                    new ScaleGestureDetector(
                            context,
                            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                @Override
                                public boolean onScale(ScaleGestureDetector detector) {
                                    float[] values = new float[9];
                                    matrix.getValues(values);
                                    float next =
                                            values[Matrix.MSCALE_X] * detector.getScaleFactor();
                                    if (next > .05f && next < 12f) {
                                        matrix.postScale(
                                                detector.getScaleFactor(),
                                                detector.getScaleFactor(),
                                                detector.getFocusX(),
                                                detector.getFocusY());
                                        setImageMatrix(matrix);
                                    }
                                    return true;
                                }
                            });
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (getDrawable() != null && !fitted && w > 0 && h > 0) {
                float fit =
                        Math.min(
                                (float) w / getDrawable().getIntrinsicWidth(),
                                (float) h / getDrawable().getIntrinsicHeight());
                matrix.setScale(fit, fit);
                matrix.postTranslate(
                        (w - getDrawable().getIntrinsicWidth() * fit) / 2,
                        (h - getDrawable().getIntrinsicHeight() * fit) / 2);
                setImageMatrix(matrix);
                fitted = true;
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            scale.onTouchEvent(e);
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                x = e.getX();
                y = e.getY();
            } else if (e.getActionMasked() == MotionEvent.ACTION_MOVE
                    && e.getPointerCount() == 1
                    && !scale.isInProgress()) {
                matrix.postTranslate(e.getX() - x, e.getY() - y);
                setImageMatrix(matrix);
                x = e.getX();
                y = e.getY();
            } else if (e.getActionMasked() == MotionEvent.ACTION_UP) performClick();
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }
    }
}
