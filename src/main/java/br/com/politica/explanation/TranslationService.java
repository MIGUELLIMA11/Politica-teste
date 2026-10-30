package br.com.politica.explanation;

import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
public class TranslationService {

    private static final String SYSTEM_PROMPT = "Act as an expert political communicator. "
            + "Translate the following Brazilian legislative proposal into simple, everyday language. "
            + "Do not use legal or economic jargon. Use practical analogies. "
            + "Format the output strictly in Brazilian Portuguese using three bold labels: "
            + "'A Situa\u00e7\u00e3o Atual', 'O Que Muda', and 'O Impacto no Seu Bolso'.";

    private final ChatModel chatModel;

    public TranslationService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String translateProposition(String originalSummary) {
        Assert.hasText(originalSummary, "originalSummary must not be blank");

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(originalSummary)));
        ChatResponse response = chatModel.call(prompt);

        if (response == null || response.getResult() == null
                || response.getResult().getOutput() == null
                || !StringUtils.hasText(response.getResult().getOutput().getText())) {
            throw new IllegalStateException("Gemini returned no translation text");
        }
        return response.getResult().getOutput().getText();
    }
}