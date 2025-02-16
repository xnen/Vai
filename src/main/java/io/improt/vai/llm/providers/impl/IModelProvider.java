package io.improt.vai.llm.providers.impl;

import java.io.File;
import java.util.List;

import io.improt.vai.llm.Cost;
import io.improt.vai.llm.chat.ChatMessage;

public interface IModelProvider {
    // Updated request method signature that accepts a reasoningEffort parameter.
    String request(String prompt, String userRequest, List<File> files);

    String chatRequest(List<ChatMessage> messages) throws Exception;
    String getModelName();

    Cost getCost();

    default String getFriendlyName() {
        return getModelName();
    }

    void init();

    boolean supportsAudio();
    boolean supportsVideo();
    boolean supportsVision();
}
