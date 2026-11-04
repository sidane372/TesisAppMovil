package com.example.santaceciliatesis.data.remote;

public final class ApiEndpoints {
    public static final String SERVICIOS = "servicios",
            LOGIN = "auth/login",
            REGISTRO = "auth/register",
            REFRESH = "auth/refresh",
            LOGOUT = "auth/logout",
            RECUPERAR = "auth/forgot-password",
            ME = "me",
            USUARIOS = "admin/usuarios",
            ADMIN_SERVICIOS = "admin/servicios",
            PROPIOS = "mis-servicios";

    private ApiEndpoints() {}

    public static String detalleServicio(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID inválido");
        return SERVICIOS + "/" + id;
    }

    public static String pagina(String ruta, int pagina, int tamano) {
        if (pagina < 0
                || tamano < 1
                || tamano > 100
                || (!ruta.equals(USUARIOS) && !ruta.equals(ADMIN_SERVICIOS)))
            throw new IllegalArgumentException("Página inválida");
        return ruta + "?pagina=" + pagina + "&tamano=" + tamano;
    }

    public static boolean permitido(String path) {
        if (path == null) return false;
        return path.equals("admin/cuentas-cobro")
                || path.matches("admin/cuentas-cobro/[1-9][0-9]*")
                || path.matches("contratos/[1-9][0-9]*/pagos")
                || path.matches("pagos(?:/[1-9][0-9]*(?:/comprobante|/cancelacion)?)?")
                || path.matches("pagos\\?pagina=[0-9]+(?:&estado=[A-Z_]+)?")
                || path.matches("admin/pagos/[1-9][0-9]*/(?:revision|reembolsos)")
                || path.matches(
                        "(?:admin/)?reembolsos/[1-9][0-9]*(?:/confirmacion|/comprobante|/rechazo)?")
                || path.matches(
                        "(?:solicitudes|contratos)(?:/[1-9][0-9]*(?:/estado|/cotizaciones(?:/[1-9][0-9]*/(?:respuesta|aceptacion))?)?)?")
                || path.matches(
                        "(?:solicitudes|contratos)\\?pagina=[0-9]+&tamano=20(?:&estado=[A-Z_]+)?(?:&q=[A-Za-z0-9%_.~-]*)?")
                || path.equals("mi-resumen")
                || path.matches(
                        "servicios/[1-9][0-9]*/(?:disponibilidad|agenda)\\?desde=[0-9]{4}-[0-9]{2}-[0-9]{2}&hasta=[0-9]{4}-[0-9]{2}-[0-9]{2}")
                || path.matches("servicios/[1-9][0-9]*/(?:horarios|bloqueos(?:/[1-9][0-9]*)?)")
                || SERVICIOS.equals(path)
                || LOGIN.equals(path)
                || REGISTRO.equals(path)
                || REFRESH.equals(path)
                || LOGOUT.equals(path)
                || RECUPERAR.equals(path)
                || ME.equals(path)
                || USUARIOS.equals(path)
                || ADMIN_SERVICIOS.equals(path)
                || PROPIOS.equals(path)
                || path.matches("servicios/[1-9][0-9]*(?:/tarifa|/repertorio(?:/[1-9][0-9]*)?)?")
                || path.matches("admin/usuarios/[1-9][0-9]*(?:/estado)?")
                || path.matches("admin/servicios/[1-9][0-9]*(?:/publicacion)?")
                || path.matches("admin/(?:usuarios|servicios)\\?pagina=[0-9]+&tamano=[1-9][0-9]?")
                || path.matches("admin/(?:usuarios|servicios)\\?pagina=[0-9]+&tamano=100");
    }
}
