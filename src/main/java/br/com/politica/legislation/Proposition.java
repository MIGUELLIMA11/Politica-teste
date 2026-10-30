package br.com.politica.legislation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "propositions")
public class Proposition {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "proposal_number", nullable = false)
    private Integer number;

    @Column(name = "proposal_year", nullable = false)
    private Integer year;

    @Column(name = "original_summary", columnDefinition = "TEXT", nullable = false)
    private String originalSummary;

    @Column(name = "translated_summary", columnDefinition = "TEXT")
    private String translatedSummary;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Proposition() {
    }

    public Proposition(Long id, String type, Integer number, Integer year, String originalSummary) {
        this.id = Objects.requireNonNull(id, "Government API ID is required");
        this.type = type;
        this.number = number;
        this.year = year;
        this.originalSummary = originalSummary;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getOriginalSummary() {
        return originalSummary;
    }

    public void setOriginalSummary(String originalSummary) {
        this.originalSummary = originalSummary;
    }

    public String getTranslatedSummary() {
        return translatedSummary;
    }

    public void setTranslatedSummary(String translatedSummary) {
        this.translatedSummary = translatedSummary;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}