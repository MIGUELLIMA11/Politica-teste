package br.com.politica.ingestion;

import br.com.politica.ingestion.model.ProposicoesResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class CamaraApiService {

    private final RestClient restClient;

    @Autowired
    public CamaraApiService(
            RestClient.Builder builder,
            @Value("${camara.api.base-url:https://dadosabertos.camara.leg.br/api/v2}") String baseUrl
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    CamaraApiService(RestClient restClient) {
        this.restClient = restClient;
    }

    public ProposicoesResponse buscarProposicoesRecentes() {
        return buscarProposicoesRecentes(1, 100);
    }

    public ProposicoesResponse buscarProposicoesRecentes(int pagina, int itens) {
        if (pagina < 1) {
            throw new IllegalArgumentException("pagina must be at least 1");
        }
        if (itens < 1 || itens > 100) {
            throw new IllegalArgumentException("itens must be between 1 and 100");
        }

        ProposicoesResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/proposicoes")
                        .queryParam("siglaTipo", "PEC,PL")
                        .queryParam("ordem", "DESC")
                        .queryParam("ordenarPor", "id")
                        .queryParam("pagina", pagina)
                        .queryParam("itens", itens)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ProposicoesResponse.class);

        if (response == null) {
            throw new RestClientException("Camara API returned an empty response body");
        }
        return response;
    }
}