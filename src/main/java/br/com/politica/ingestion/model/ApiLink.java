package br.com.politica.ingestion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiLink(String rel, URI href) {
}