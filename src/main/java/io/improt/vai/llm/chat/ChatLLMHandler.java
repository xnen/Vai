package io.improt.vai.llm.chat;

import com.openai.models.*;
import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.content.AudioContent;
import io.improt.vai.llm.chat.content.ChatMessageUserType;
import io.improt.vai.llm.chat.content.ImageContent;
import io.improt.vai.llm.chat.content.TextContent;
import io.improt.vai.llm.providers.IModelProvider;
import io.improt.vai.llm.util.OpenAIUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * ChatLLMHandler manages the chat conversation history and interacts with the underlying LLM providers.
 * It assembles conversation history into a request that includes previous messages and delegates the call
 * to the appropriate provider based on the selected model.
 *
 * Supported models include:
 * - "o3-mini (Text Only)"
 * - "o1 (Text, Image)"
 * - "gpt-4o-audio-preview (Text, Voice)"
 * - "gpt-4o-mini-audio-preview (Text, Voice)"
 */
public class ChatLLMHandler {
    private final List<ChatMessage> conversationHistory = new ArrayList<>();
    private String selectedModel;

    public ChatLLMHandler(String selectedModel) {
        this.selectedModel = selectedModel;
    }

    /**
     * Adds a new message to the conversation history.
     *
     * @param message The chat message to add.
     */
    public void addMessage(ChatMessage message) {
        conversationHistory.add(message);
    }

    /**
     * Returns the complete conversation history.
     *
     * @return List of ChatMessage objects.
     */
    public List<ChatMessage> getConversationHistory() {
        return conversationHistory;
    }

    public void setSelectedModel(String model) {
        this.selectedModel = model;
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public void runModelWithCurrentHistory() throws Exception {
        IModelProvider provider = App.getInstance().getLLMProvider(selectedModel);
        String response = provider.chatRequest(this.conversationHistory);
        addMessage(new ChatMessage(ChatMessageUserType.ASSISTANT, new TextContent(response)));
    }
}
