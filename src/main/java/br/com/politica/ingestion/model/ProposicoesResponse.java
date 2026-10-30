package br.com.politica.ingestion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProposicoesResponse(List<Proposicao> dados, List<ApiLink> links) {

    public ProposicoesResponse {
        dados = List.copyOf(Objects.requireNonNull(dados, "Response must contain dados"));
        links = links == null ? List.of() : List.copyOf(links);
    }

    public boolean hasNextPage() {
        return links.stream().anyMatch(link -> "next".equals(link.rel()));
    }
}