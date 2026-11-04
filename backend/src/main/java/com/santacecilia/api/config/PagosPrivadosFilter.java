package com.santacecilia.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;

/** Cuerpos de comprobante acotados antes de deserializar; ninguna imagen es pública o cacheable. */
@Component
public class PagosPrivadosFilter extends OncePerRequestFilter {
    private static final int LIMIT = 3 * 1024 * 1024;

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String path = req.getRequestURI();
        if (path.startsWith("/api/v1/pagos")
                || path.startsWith("/api/v1/reembolsos")
                || path.startsWith("/api/v1/admin/")
                || path.matches("/api/v1/contratos/[0-9]+/pagos")) {
            res.setHeader("Cache-Control", "no-store");
            res.setHeader("Pragma", "no-cache");
        }
        if ((path.matches("/api/v1/pagos/[0-9]+/comprobante") && req.getMethod().equals("PUT"))
                || path.matches("/api/v1/admin/reembolsos/[0-9]+/confirmacion")) {
            if (req.getContentLengthLong() > LIMIT) {
                res.sendError(413);
                return;
            }
            chain.doFilter(
                    new HttpServletRequestWrapper(req) {
                        @Override
                        public ServletInputStream getInputStream() throws IOException {
                            ServletInputStream original = super.getInputStream();
                            return new ServletInputStream() {
                                int count;

                                private void check(int n) throws IOException {
                                    if (n > 0 && (count += n) > LIMIT)
                                        throw new IOException("Cuerpo demasiado grande");
                                }

                                public int read() throws IOException {
                                    int v = original.read();
                                    check(v < 0 ? 0 : 1);
                                    return v;
                                }

                                public int read(byte[] b, int off, int len) throws IOException {
                                    int n = original.read(b, off, len);
                                    check(n);
                                    return n;
                                }

                                public boolean isFinished() {
                                    return original.isFinished();
                                }

                                public boolean isReady() {
                                    return original.isReady();
                                }

                                public void setReadListener(ReadListener listener) {
                                    original.setReadListener(listener);
                                }

                                public void close() throws IOException {
                                    original.close();
                                }
                            };
                        }
                    },
                    res);
            return;
        }
        chain.doFilter(req, res);
    }
}
