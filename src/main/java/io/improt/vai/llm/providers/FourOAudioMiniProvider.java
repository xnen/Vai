package io.improt.vai.llm.providers;

import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class FourOAudioMiniProvider extends OpenAIClientBase implements IModelProvider {

    public FourOAudioMiniProvider() {
        super("gpt-4o-mini-audio-preview");
    }

    @Override
    public boolean supportsAudio() {
        return true;
    }
}
