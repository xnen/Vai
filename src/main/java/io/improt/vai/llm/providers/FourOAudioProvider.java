package io.improt.vai.llm.providers;

import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class FourOAudioProvider extends OpenAIClientBase implements IModelProvider {

    public FourOAudioProvider() {
        super("gpt-4o-audio-preview");
    }

    @Override
    public boolean supportsAudio() {
        return true;
    }
}
