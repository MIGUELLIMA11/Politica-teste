package br.com.politica.ingestion;

import br.com.politica.explanation.TranslationService;
import br.com.politica.legislation.Proposition;
import br.com.politica.legislation.PropositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CamaraStartupRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CamaraStartupRunner.class);

    private final CamaraApiService camaraApiService;
    private final PropositionRepository propositionRepository;
    private final TranslationService translationService;

    public CamaraStartupRunner(CamaraApiService camaraApiService,
                               PropositionRepository propositionRepository,
                               TranslationService translationService) {
        this.camaraApiService = camaraApiService;
        this.propositionRepository = propositionRepository;
        this.translationService = translationService;
    }

    @Override
    public void run(String... args) {
        var response = camaraApiService.buscarProposicoesRecentes();
        logger.info("Fetched {} propositions from the Camara API (first page)", response.dados().size());

        for (var proposicao : response.dados()) {
            Proposition proposition = propositionRepository.findById(proposicao.id())
                    .orElseGet(() -> new Proposition(proposicao.id(), proposicao.siglaTipo(),
                            proposicao.numero(), proposicao.ano(), proposicao.ementa()));
            proposition.setType(proposicao.siglaTipo());
            proposition.setNumber(proposicao.numero());
            proposition.setYear(proposicao.ano());
            proposition.setOriginalSummary(proposicao.ementa());

            try {
                String translation = translationService.translateProposition(proposition.getOriginalSummary());
                proposition.setTranslatedSummary(translation);
            } catch (RuntimeException exception) {
                logger.warn("Translation failed for proposition ID={} ({}); saving raw data without a new translation",
                        proposition.getId(), exception.getClass().getSimpleName());
            }

            propositionRepository.save(proposition);
            logger.info("Saved proposition ID={} | Type={}", proposition.getId(), proposition.getType());
        }
        logger.info("Saved {} propositions to the database", response.dados().size());
    }
}