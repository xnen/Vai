package io.improt.vai.llm.providers;

import java.io.File;
import java.util.List;
import com.openai.models.ChatCompletionReasoningEffort;

public interface IModelProvider {
    // Updated request method signature that accepts a reasoningEffort parameter.
    String request(String model, String prompt, String userRequest, List<File> files, ChatCompletionReasoningEffort reasoningEffort);
    void init();

    boolean supportsAudio();
    boolean supportsVideo();
    boolean supportsVision();
    
    // New method to indicate whether this model supports a custom reasoning effort
    boolean supportsReasoningEffort();
}
