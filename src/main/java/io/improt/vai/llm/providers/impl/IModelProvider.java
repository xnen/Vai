package io.improt.vai.llm.providers.impl;

import java.io.File;
import java.util.List;

import io.improt.vai.llm.Cost;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.util.stream.ISnippetAction;

public interface IModelProvider {
    // Updated request method signature that accepts a reasoningEffort parameter.
    String request(String prompt, String userRequest, List<File> files);

    String chatRequest(List<ChatMessage> messages) throws Exception;

    /**
     * Performs a streaming chat request.
     *
     * @param messages The list of messages forming the conversation history.
     * @param streamAction The action to perform for each received snippet of the response.
     * @param onComplete A Runnable to execute when the streaming response is fully processed.
     * @throws Exception If an error occurs during the streaming request.
     */
    void streamChatRequest(List<ChatMessage> messages, ISnippetAction streamAction, Runnable onComplete) throws Exception;

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

