package br.com.politica.ingestion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Proposicao(
        Long id,
        URI uri,
        String siglaTipo,
        Integer codTipo,
        Integer numero,
        Integer ano,
        String ementa,
        LocalDateTime dataApresentacao
) {
}