package br.com.politica.explanation;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TranslationServiceTest {

    private final ChatModel chatModel = mock(ChatModel.class);
    private final TranslationService service = new TranslationService(chatModel);

    @Test
    void sendsExactSystemPromptAndSeparateOriginalSummary() {
        String original = "Altera a tributacao em {10%}.";
        String translation = "**A Situa\u00e7\u00e3o Atual**\nTexto atual.\n"
                + "**O Que Muda**\nTexto proposto.\n**O Impacto no Seu Bolso**\nImpacto possivel.";
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage(translation)))));

        assertThat(service.translateProposition(original)).isEqualTo(translation);

        var captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        var messages = captor.getValue().getInstructions();
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(messages.get(0).getText()).isEqualTo(
                "Act as an expert political communicator. Translate the following Brazilian legislative "
                + "proposal into simple, everyday language. Do not use legal or economic jargon. "
                + "Use practical analogies. Format the output strictly in Brazilian Portuguese using "
                + "three bold labels: 'A Situa\u00e7\u00e3o Atual', 'O Que Muda', and 'O Impacto no Seu Bolso'.");
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(1).getText()).isEqualTo(original);
    }

    @Test
    void rejectsBlankInputWithoutCallingGemini() {
        for (String summary : new String[] {null, "", "  \n"}) {
            assertThatThrownBy(() -> service.translateProposition(summary))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(chatModel);
    }

    @Test
    void rejectsMissingResponse() {
        assertThatThrownBy(() -> service.translateProposition("Official summary"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gemini returned no translation text");
    }

    @Test
    void rejectsEmptyGenerations() {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of()));

        assertThatThrownBy(() -> service.translateProposition("Official summary"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsBlankTranslation() {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage(" ")))));

        assertThatThrownBy(() -> service.translateProposition("Official summary"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void propagatesProviderFailure() {
        var failure = new IllegalStateException("Provider unavailable");
        when(chatModel.call(any(Prompt.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.translateProposition("Official summary"))
                .isSameAs(failure);
    }
}