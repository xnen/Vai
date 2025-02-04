package io.improt.vai.llm.providers;

import java.io.File;
import java.util.List;

public class FourOAudioMiniProvider extends OpenAICommons implements IModelProvider {

    public FourOAudioMiniProvider() {
        super();
    }

    @Override
    public String request(String prompt, String userRequest, List<File> files) {
        return null;
    }

    @Override
    protected boolean supportsDeveloperRole() {
        return false;
    }

    @Override
    public String getModelName() {
        return "gpt-4o-mini-audio-preview";
    }

    @Override
    public boolean supportsReasoningEffort() {
        return false;
    }

    @Override
    public boolean supportsAudio() {
        return true;
    }

    @Override
    public boolean supportsVideo() {
        return false;
    }

    @Override
    public boolean supportsVision() {
        return false;
    }
}
