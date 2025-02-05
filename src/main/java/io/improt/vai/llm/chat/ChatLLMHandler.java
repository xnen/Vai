package io.improt.vai.llm.chat;

import com.openai.models.*;
import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.content.ChatMessageUserType;
import io.improt.vai.llm.chat.content.TextContent;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.O3MiniProvider;
import io.improt.vai.llm.providers.openai.utils.Messages;
import io.improt.vai.mapping.WorkspaceMapper;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatLLMHandler manages the chat conversation history and interacts with the underlying LLM providers.
 * It assembles conversation history into a request that includes previous messages and delegates the call
 * to the appropriate provider based on the selected model.
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
        if (provider == null) {
            System.out.println("Provider was not found for '" + selectedModel + "'.");
        }
        if (this.conversationHistory.isEmpty()) {
            System.out.println("Conversation history empty!");
            return;
        }

        // Hack to include repository mappings if the user adds !askrepo to their message.
        boolean includeRepoContext = false;
        ChatMessage latestMessage = this.conversationHistory.get(this.conversationHistory.size() - 1);
        if (latestMessage.getContent() instanceof TextContent) {
            String msg = latestMessage.getContent().toString();
            if (msg.contains("!askrepo")) {
                ((TextContent) latestMessage.getContent()).setText(msg.replace("!askrepo", ""));
                includeRepoContext = true;
            }
        }


        List<ChatMessage> newHistory;
        if (includeRepoContext) {
            newHistory = new ArrayList<>();
            WorkspaceMapper mapper = new WorkspaceMapper();
            String mappings = mapper.getAllMappingsConcatenated();
            newHistory.add(new ChatMessage(ChatMessageUserType.USER, new TextContent("The following is a map of the user's repository. Use it to answer questions:\n" + mappings)));
            newHistory.addAll(conversationHistory);
        } else {
            newHistory = new ArrayList<>(conversationHistory);
        }

        String response = provider.chatRequest(newHistory);

        System.out.println(response);

        if (response.contains("<end_message>")) {
            String[] msgs = response.split("<end_message>");
            for (String message : msgs) {
                String trimmedMessage = message.trim(); // Removes leading and trailing whitespace
                addMessage(new ChatMessage(ChatMessageUserType.ASSISTANT, new TextContent(trimmedMessage)));
            }
        } else {
            addMessage(new ChatMessage(ChatMessageUserType.ASSISTANT, new TextContent(response)));
        }
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

    public boolean streamAlive = true;
    public void doKeyboardStreaming() {
        O3MiniProvider provider = (O3MiniProvider) App.getInstance().getLLMProvider("o3-mini");
        try {
            ChatCompletionCreateParams.Builder builder = Messages.buildChat(provider, this.conversationHistory);
            ChatCompletionCreateParams build = builder.model(provider.getModelName())
                    .reasoningEffort(App.getInstance().getConfiguredReasoningEffort())
                    .build();

            Robot robot = new Robot();

            provider.stream(build, (e) -> {
                if (!streamAlive)
                    return;
                try {
                    doPaste(robot, e);
                    Thread.sleep(100L);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }, 50);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doPaste(Robot robot, String string) throws InterruptedException {
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
        Thread.sleep(50L);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        Thread.sleep(10L);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        System.out.println("Should've pasted " + string);
    }

    public void killStream() {
        this.streamAlive = false;
    }
}
