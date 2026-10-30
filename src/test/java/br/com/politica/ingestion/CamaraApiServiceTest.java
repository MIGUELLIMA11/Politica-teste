package br.com.politica.ingestion;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CamaraApiServiceTest {

    private MockRestServiceServer server;
    private CamaraApiService service;

    @BeforeEach
    void setUp() {
                RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test/api/v2");
        server = MockRestServiceServer.bindTo(builder).build();
                service = new CamaraApiService(builder.build());
    }

    @Test
    void fetchesPecAndPlNewestFirstAndMapsResponse() {
        server.expect(requestTo("https://example.test/api/v2/proposicoes?siglaTipo=PEC,PL&ordem=DESC&ordenarPor=id&pagina=1&itens=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Accept", "application/json"))
                .andRespond(withSuccess("""
                        {
                          "dados": [
                            {"id": 123, "uri": "https://example.test/proposicoes/123",
                             "siglaTipo": "PEC", "codTipo": 136, "numero": 1,
                             "ano": 2026, "ementa": "Altera a tributa\\u00e7\\u00e3o.",
                             "dataApresentacao": "2026-10-29T16:27", "futureField": true},
                            {"id": 122, "siglaTipo": "PL", "ementa": "Altera regras."}
                          ],
                          "links": [{"rel": "next", "href": "https://example.test?page=2"}]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = service.buscarProposicoesRecentes();

        assertThat(response.dados()).hasSize(2);
        assertThat(response.dados().get(0).id()).isEqualTo(123L);
        assertThat(response.dados().get(0).siglaTipo()).isEqualTo("PEC");
        assertThat(response.dados().get(0).ementa()).isEqualTo("Altera a tributa\u00e7\u00e3o.");
        assertThat(response.dados().get(0).dataApresentacao())
                .isEqualTo(LocalDateTime.of(2026, 10, 29, 16, 27));
        assertThat(response.dados().get(1).dataApresentacao()).isNull();
        assertThat(response.hasNextPage()).isTrue();
        server.verify();
    }

    @Test
    void supportsExplicitPaginationAndEmptyResults() {
        server.expect(queryParam("pagina", "2"))
                .andExpect(queryParam("itens", "20"))
                .andRespond(withSuccess("{\"dados\":[],\"links\":[]}", MediaType.APPLICATION_JSON));

        var response = service.buscarProposicoesRecentes(2, 20);

        assertThat(response.dados()).isEmpty();
        assertThat(response.hasNextPage()).isFalse();
        server.verify();
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> service.buscarProposicoesRecentes(0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscarProposicoesRecentes(1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscarProposicoesRecentes(1, 101))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @Test
    void propagatesHttpErrorsRatherThanReturningEmptyResults() {
        server.expect(method(HttpMethod.GET)).andRespond(withServerError());

        assertThatThrownBy(() -> service.buscarProposicoesRecentes())
                .isInstanceOf(HttpServerErrorException.class);
        server.verify();
    }

    @Test
    void rejectsEmptyResponseBody() {
        server.expect(method(HttpMethod.GET)).andRespond(withSuccess());

        assertThatThrownBy(() -> service.buscarProposicoesRecentes())
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("empty response body");
        server.verify();
    }
}