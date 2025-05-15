package io.improt.vai.llm.chat;

import com.openai.models.*;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.content.ChatMessageUserType;
import io.improt.vai.llm.chat.content.TextContent;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.O3MiniProvider;
import io.improt.vai.llm.providers.openai.utils.Messages;
import io.improt.vai.mapping.WorkspaceMapper;
import io.improt.vai.util.stream.ISnippetAction;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatLLMHandler manages the chat conversation history and interacts with the underlying LLM providers.
 * It assembles conversation history into a request that includes previous messages and delegates the call
 * to the appropriate provider based on the selected model. Supports both blocking and streaming requests.
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

    /**
     * Runs the model request in a blocking manner with the current history.
     * Appends the assistant's response(s) to the conversation history.
     * Handles the !askrepo command to inject repository context.
     *
     * @throws Exception if the model request fails.
     */
    public void runModelWithCurrentHistory() throws Exception {
        IModelProvider provider = App.getInstance().getLLMProvider(selectedModel);
        if (this.conversationHistory.isEmpty()) {
            System.out.println("Conversation history empty!");
            return;
        }

        List<ChatMessage> historyForRequest = prepareHistoryForRequest();
        String response = provider.chatRequest(historyForRequest);

        System.out.println("=== CHATLLM RESPONSE (Blocking) ===");
        System.out.println(response);
        System.out.println("=== === === === === === ===");

        // Add the raw response as a single message
        if (response != null && !response.trim().isEmpty()) {
            addMessage(new ChatMessage(ChatMessageUserType.ASSISTANT, new TextContent(response.trim())));
        }
    }

    /**
     * Runs the model request in a streaming manner with the current history.
     * Handles the !askrepo command to inject repository context.
     *
     * @param streamAction The action to perform for each received snippet.
     * @param onComplete   A Runnable to execute when the streaming is complete.
     * @throws Exception if initiating the stream request fails.
     */
    public void streamModelResponse(ISnippetAction streamAction, Runnable onComplete) throws Exception {
        IModelProvider provider = App.getInstance().getLLMProvider(selectedModel);
        if (this.conversationHistory.isEmpty()) {
            System.out.println("Conversation history empty!");
            if (onComplete != null) onComplete.run(); // Ensure completion callback if history is empty
            return;
        }

        List<ChatMessage> historyForRequest = prepareHistoryForRequest();
        System.out.println("Starting stream request...");
        provider.streamChatRequest(historyForRequest, streamAction, onComplete);
    }

    /**
     * Prepares the conversation history for sending to the model.
     * Handles the !askrepo command by injecting repository context if present.
     *
     * @return The list of ChatMessages to send to the model.
     */
    private List<ChatMessage> prepareHistoryForRequest() {
        boolean includeRepoContext = false;
        if (!this.conversationHistory.isEmpty()) {
            ChatMessage latestMessage = this.conversationHistory.get(this.conversationHistory.size() - 1);
            if (latestMessage.getContent() instanceof TextContent) {
                String msg = latestMessage.getContent().toString();
                if (msg.contains("!askrepo")) {
                    // Modify the last message to remove the command before sending
                    ((TextContent) latestMessage.getContent()).setText(msg.replace("!askrepo", ""));
                    includeRepoContext = true;
                }
            }
        }

        List<ChatMessage> historyToSend;
        if (includeRepoContext) {
            historyToSend = new ArrayList<>();
            WorkspaceMapper mapper = new WorkspaceMapper(App.getInstance().getCurrentWorkspace());
            String mappings = mapper.getAllMappingsConcatenated();
            // Inject context as a *user* message right before the actual user message that contained !askrepo
             if (conversationHistory.size() > 1) {
                 historyToSend.addAll(conversationHistory.subList(0, conversationHistory.size() - 1));
             }
             historyToSend.add(new ChatMessage(ChatMessageUserType.USER, new TextContent("Here is my repository context:\n" + mappings)));
             // Add the modified last user message
             historyToSend.add(conversationHistory.get(conversationHistory.size()-1));
        } else {
            historyToSend = new ArrayList<>(conversationHistory);
        }
        return historyToSend;
    }

    /**
     * Removes the specified message from the conversation history.
     * (Used when a chat bubble is double-clicked to be removed.)
     *
     * @param message The message to remove.
     */
    public void removeMessage(ChatMessage message) {
        conversationHistory.remove(message);
    }
    
    // Removed streamAlive, doPaste, and killStream as they were related to keyboard mode.
}
