package com.santacecilia.api.dto;
import java.util.List;
public record Pagina<T>(List<T> items,int pagina,int tamano,long total) {}
