package br.com.politica.legislation;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:propositions",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PropositionRepositoryTest {

    @Autowired
    private PropositionRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void preservesGovernmentIdAndLongOriginalSummary() {
        String summary = "Texto oficial. ".repeat(1000);
        repository.saveAndFlush(new Proposition(2648653L, "PEC", 10, 2026, summary));
        entityManager.clear();

        Proposition stored = repository.findById(2648653L).orElseThrow();

        assertThat(stored.getId()).isEqualTo(2648653L);
        assertThat(stored.getType()).isEqualTo("PEC");
        assertThat(stored.getNumber()).isEqualTo(10);
        assertThat(stored.getYear()).isEqualTo(2026);
        assertThat(stored.getOriginalSummary()).isEqualTo(summary);
        assertThat(stored.getTranslatedSummary()).isNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
    }

    @Test
    void updatesExistingGovernmentIdAndStoresLongTranslation() {
        Proposition stored = repository.saveAndFlush(
                new Proposition(2649633L, "PL", 5704, 2026, "Original summary"));
        var previousTimestamp = stored.getUpdatedAt();
        entityManager.clear();
        String translation = "Explicacao simples. ".repeat(1000);
        Proposition replacement = new Proposition(2649633L, "PL", 5704, 2026, "Updated summary");
        replacement.setTranslatedSummary(translation);

        repository.saveAndFlush(replacement);
        entityManager.clear();

        Proposition updated = repository.findById(2649633L).orElseThrow();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(updated.getOriginalSummary()).isEqualTo("Updated summary");
        assertThat(updated.getTranslatedSummary()).isEqualTo(translation);
        assertThat(updated.getUpdatedAt()).isAfter(previousTimestamp);
    }

    @Test
    void requiresGovernmentId() {
        assertThatThrownBy(() -> new Proposition(null, "PL", 1, 2026, "Summary"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Government API ID");
    }
}