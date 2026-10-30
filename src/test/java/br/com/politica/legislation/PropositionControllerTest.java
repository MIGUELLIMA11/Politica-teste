package br.com.politica.legislation;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PropositionController.class)
class PropositionControllerTest {

    @MockitoBean
    private PropositionRepository repository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsPropositionsUsingNewestUpdatedAtSortAndCors() throws Exception {
        Proposition latest = new Proposition(123L, "PEC", 1, 2026, "Official summary");
        latest.setTranslatedSummary("Everyday explanation");
        ReflectionTestUtils.setField(latest, "updatedAt", Instant.parse("2026-10-30T12:00:00Z"));
        Proposition older = new Proposition(122L, "PL", 2, 2026, "Older summary");
        ReflectionTestUtils.setField(older, "updatedAt", Instant.parse("2026-10-29T12:00:00Z"));
        Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt");
        when(repository.findAll(sort)).thenReturn(List.of(latest, older));

        mockMvc.perform(get("/api/propositions").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(123))
                .andExpect(jsonPath("$[0].type").value("PEC"))
                .andExpect(jsonPath("$[0].number").value(1))
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[0].originalSummary").value("Official summary"))
                .andExpect(jsonPath("$[0].translatedSummary").value("Everyday explanation"))
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[1].id").value(122));

        verify(repository).findAll(sort);
    }

    @Test
    void returnsEmptyJsonArrayWhenNoPropositionsExist() throws Exception {
        when(repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))).thenReturn(List.of());

        mockMvc.perform(get("/api/propositions"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void acceptsCorsPreflightForGet() throws Exception {
        mockMvc.perform(options("/api/propositions")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET"));

        verifyNoInteractions(repository);
    }
}