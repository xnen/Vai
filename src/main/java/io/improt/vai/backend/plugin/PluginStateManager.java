package io.improt.vai.backend.plugin;

import io.improt.vai.util.Constants;
import io.improt.vai.util.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PluginStateManager {
    public static Map<String, Boolean> loadStates() {
        Map<String, Boolean> states = new HashMap<>();
        File stateFile = new File(Constants.PLUGIN_STATE_FILE);
        if (stateFile.exists()) {
            String content = FileUtils.readFileToString(stateFile);
            if (content != null && !content.isEmpty()) {
                try {
                    JSONObject json = new JSONObject(content);
                    for (String key : json.keySet()) {
                        states.put(key, json.getBoolean(key));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return states;
    }
    
    public static void saveStates(Iterable<AbstractPlugin> plugins) {
        JSONObject json = new JSONObject();
        for (AbstractPlugin plugin : plugins) {
            json.put(plugin.getIdentifier(), plugin.isActive());
        }
        FileUtils.writeStringToFile(new File(Constants.PLUGIN_STATE_FILE), json.toString(4));
    }
}
