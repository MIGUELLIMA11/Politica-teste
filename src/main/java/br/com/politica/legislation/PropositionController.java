package br.com.politica.legislation;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/propositions")
@CrossOrigin(origins = "*")
public class PropositionController {

    private final PropositionRepository propositionRepository;

    public PropositionController(PropositionRepository propositionRepository) {
        this.propositionRepository = propositionRepository;
    }

    @GetMapping
    public List<Proposition> getPropositions() {
        return propositionRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }
}