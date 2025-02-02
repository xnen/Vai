package io.improt.vai.util;

import io.improt.vai.backend.App;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the message history for the current workspace.
 * The history is stored in a JSON file ("message_history.json") located in the workspace-specific VAI directory.
 */
public class MessageHistoryManager {
    private final List<String> history;
    private int currentIndex;
    private final File historyFile;

    public MessageHistoryManager(File workspace) {
        history = new ArrayList<>();
        // Get the workspace-specific VAI directory using FileUtils.
        File vaiDir = FileUtils.getWorkspaceVaiDir(workspace);
        historyFile = new File(vaiDir, "message_history.json");
        loadHistory();
        // Set current index to the most recent message.
        currentIndex = history.size() - 1;
    }

    private void loadHistory() {
        if (historyFile.exists()) {
            String jsonContent = FileUtils.readFileToString(historyFile);
            if (jsonContent != null && !jsonContent.isEmpty()) {
                try {
                    JSONArray jsonArray = new JSONArray(jsonContent);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        history.add(jsonArray.getString(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveHistory() {
        JSONArray jsonArray = new JSONArray();
        for (String message : history) {
            jsonArray.put(message);
        }
        FileUtils.writeStringToFile(historyFile, jsonArray.toString(4));
    }

    /**
     * Adds a message to the history and resets the current index to the latest message.
     */
    public void addMessage(String message) {
        history.add(message);
        currentIndex = history.size() - 1;
        saveHistory();
    }

    /**
     * Returns the current message being displayed.
     */
    public String getCurrentMessage() {
        if (history.isEmpty()) {
            return "";
        }
        return history.get(currentIndex);
    }

    /**
     * Returns the previous message in history, if available.
     */
    public String getPreviousMessage() {
        if (history.isEmpty() || currentIndex <= 0) {
            return getCurrentMessage();
        }
        currentIndex--;
        return history.get(currentIndex);
    }

    /**
     * Returns the next message in history, if available.
     */
    public String getNextMessage() {
        if (history.isEmpty() || currentIndex >= history.size() - 1) {
            return getCurrentMessage();
        }
        currentIndex++;
        return history.get(currentIndex);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getTotalMessages() {
        return history.size();
    }
}
