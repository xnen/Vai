package io.improt.vai.llm.providers;

import io.improt.vai.backend.App;
import io.improt.vai.llm.Cost;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class GeminiProProvider extends OpenAIClientBase {

    public GeminiProProvider() {
        super("https://generativelanguage.googleapis.com/v1beta/openai/", "gemini-2.5-pro-preview-05-06", App.getGeminiKey());
    }

    @Override
    public String getFriendlyName() {
        return "Gemini Pro";
    }

    @Override
    public Cost getCost() {
        return Cost.LOW; // free credits.
    }

    @Override
    public boolean supportsReasoningEffort() {
        return true;
    }

    @Override
    public boolean supportsVision() {
        return true;
    }

    @Override
    public boolean supportsAudio() {
        return true;
    }


}
