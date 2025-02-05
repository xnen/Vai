package io.improt.vai.llm.providers;

import io.improt.vai.llm.Cost;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class FourOAudioMiniProvider extends OpenAIClientBase implements IModelProvider {

    public FourOAudioMiniProvider() {
        super("gpt-4o-mini-audio-preview");
    }

    @Override
    public Cost getCost() {
        return Cost.LOW;
    }

    @Override
    public String getFriendlyName() {
        return "gpt-4o-mini-audio";
    }

    @Override
    public boolean supportsAudio() {
        return true;
    }
}
