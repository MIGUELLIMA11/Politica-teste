package br.com.politica.ingestion;

import br.com.politica.explanation.TranslationService;
import br.com.politica.ingestion.model.Proposicao;
import br.com.politica.ingestion.model.ProposicoesResponse;
import br.com.politica.legislation.Proposition;
import br.com.politica.legislation.PropositionRepository;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class CamaraStartupRunnerTest {

    private final CamaraApiService service = mock(CamaraApiService.class);
    private final PropositionRepository repository = mock(PropositionRepository.class);
    private final TranslationService translationService = mock(TranslationService.class);
    private final CamaraStartupRunner runner = new CamaraStartupRunner(service, repository, translationService);

    @Test
    void mapsAndSavesFetchedPropositions(CapturedOutput output) {
        when(service.buscarProposicoesRecentes()).thenReturn(new ProposicoesResponse(List.of(
                new Proposicao(123L, null, "PEC", 136, 1, 2026, "Altera a tributacao.", null),
                new Proposicao(122L, null, "PL", 139, 2, 2026, "Altera regras fiscais.", null)
        ), List.of()));
            when(translationService.translateProposition("Altera a tributacao."))
                .thenReturn("Explicacao sobre tributos.");
            when(translationService.translateProposition("Altera regras fiscais."))
                .thenReturn("Explicacao sobre regras fiscais.");

        runner.run();

        verify(service).buscarProposicoesRecentes();
            verify(translationService).translateProposition("Altera a tributacao.");
            verify(translationService).translateProposition("Altera regras fiscais.");
        var captor = ArgumentCaptor.forClass(Proposition.class);
        verify(repository, times(2)).save(captor.capture());
        var saved = captor.getAllValues();
        assertThat(saved.get(0).getId()).isEqualTo(123L);
        assertThat(saved.get(0).getType()).isEqualTo("PEC");
        assertThat(saved.get(0).getNumber()).isEqualTo(1);
        assertThat(saved.get(0).getYear()).isEqualTo(2026);
        assertThat(saved.get(0).getOriginalSummary()).isEqualTo("Altera a tributacao.");
        assertThat(saved.get(0).getTranslatedSummary()).isEqualTo("Explicacao sobre tributos.");
        assertThat(saved.get(1).getId()).isEqualTo(122L);
        assertThat(saved.get(1).getType()).isEqualTo("PL");
        assertThat(saved.get(1).getNumber()).isEqualTo(2);
        assertThat(saved.get(1).getYear()).isEqualTo(2026);
        assertThat(saved.get(1).getOriginalSummary()).isEqualTo("Altera regras fiscais.");
        assertThat(saved.get(1).getTranslatedSummary()).isEqualTo("Explicacao sobre regras fiscais.");
        assertThat(output.getOut())
                .contains("Fetched 2 propositions")
            .contains("Saved proposition ID=123 | Type=PEC")
            .contains("Saved proposition ID=122 | Type=PL")
            .contains("Saved 2 propositions to the database");
        }

        @Test
        void updatesExistingPropositionWithoutLosingTranslation() {
        Proposition existing = new Proposition(123L, "PL", 1, 2025, "Old summary");
        existing.setTranslatedSummary("Existing translation");
        when(repository.findById(123L)).thenReturn(Optional.of(existing));
        when(service.buscarProposicoesRecentes()).thenReturn(new ProposicoesResponse(List.of(
            new Proposicao(123L, null, "PEC", 136, 2, 2026, "Updated summary", null)
        ), List.of()));
        when(translationService.translateProposition("Updated summary"))
            .thenThrow(new IllegalStateException("Provider unavailable"));

        runner.run();

        verify(repository).save(existing);
        assertThat(existing.getId()).isEqualTo(123L);
        assertThat(existing.getType()).isEqualTo("PEC");
        assertThat(existing.getNumber()).isEqualTo(2);
        assertThat(existing.getYear()).isEqualTo(2026);
        assertThat(existing.getOriginalSummary()).isEqualTo("Updated summary");
        assertThat(existing.getTranslatedSummary()).isEqualTo("Existing translation");
    }

        @Test
        void savesRawDataOnTimeoutAndContinuesWithNextProposition(CapturedOutput output) {
        when(service.buscarProposicoesRecentes()).thenReturn(new ProposicoesResponse(List.of(
            new Proposicao(123L, null, "PEC", 136, 1, 2026, "First summary", null),
            new Proposicao(122L, null, "PL", 139, 2, 2026, "Second summary", null)
        ), List.of()));
        when(translationService.translateProposition("First summary"))
            .thenThrow(new ResourceAccessException("Request timed out", new SocketTimeoutException()));
        when(translationService.translateProposition("Second summary")).thenReturn("Translated second summary");

        runner.run();

        var captor = ArgumentCaptor.forClass(Proposition.class);
        verify(repository, times(2)).save(captor.capture());
        var saved = captor.getAllValues();
        assertThat(saved.get(0).getId()).isEqualTo(123L);
        assertThat(saved.get(0).getOriginalSummary()).isEqualTo("First summary");
        assertThat(saved.get(0).getTranslatedSummary()).isNull();
        assertThat(saved.get(1).getId()).isEqualTo(122L);
        assertThat(saved.get(1).getTranslatedSummary()).isEqualTo("Translated second summary");
        assertThat(output.getOut()).contains("Translation failed for proposition ID=123")
            .contains("Saved 2 propositions to the database");
        }

        @Test
        void doesNotMisreportDatabaseFailureAsTranslationFailure(CapturedOutput output) {
        when(service.buscarProposicoesRecentes()).thenReturn(new ProposicoesResponse(List.of(
            new Proposicao(123L, null, "PEC", 136, 1, 2026, "Official summary", null)
        ), List.of()));
        when(translationService.translateProposition("Official summary")).thenReturn("Translation");
        var failure = new IllegalStateException("Database unavailable");
        when(repository.save(any(Proposition.class))).thenThrow(failure);

        assertThatThrownBy(() -> runner.run()).isSameAs(failure);
        assertThat(output.getOut()).doesNotContain("Translation failed").doesNotContain("Saved proposition");
        }

    @Test
    void logsEmptyResults(CapturedOutput output) {
        when(service.buscarProposicoesRecentes())
                .thenReturn(new ProposicoesResponse(List.of(), List.of()));

        runner.run();

        verify(service).buscarProposicoesRecentes();
        verifyNoInteractions(repository, translationService);
        assertThat(output.getOut()).contains("Fetched 0 propositions").doesNotContain("ID=");
    }
}