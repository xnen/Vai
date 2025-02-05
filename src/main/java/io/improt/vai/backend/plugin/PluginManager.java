package io.improt.vai.backend.plugin;

import io.improt.vai.backend.plugin.impl.*;

import java.util.List;

public class PluginManager {
    private static PluginManager instance;  // Singleton instance
    private final List<AbstractPlugin> pluginList;

    public PluginManager() {
        pluginList = List.of(
                new ShowMessagePlugin(),
                new RunCommandPlugin(),
                new LLMPromptPlugin(),
                new RequestPlanPlugin(),
                new AutoLLMScanPlugin()
        );
        
        // Load previously saved plugin states and update each plugin
        var savedStates = PluginStateManager.loadStates();
        for (AbstractPlugin plugin : pluginList) {
            String id = plugin.getIdentifier();
            if (savedStates.containsKey(id)) {
                plugin.setActive(savedStates.get(id));
            }
        }
        
        instance = this;
    }
    
    public static PluginManager getInstance() {
        return instance;
    }
    
    public List<AbstractPlugin> getPlugins() {
        return pluginList;
    }

    public boolean passResponse(String fileName, String type, String response) {
        for (AbstractPlugin plugin : pluginList) {
            if (!plugin.isActive()) {
                continue;
            }
            if (plugin.getIdentifier().equals(fileName.toUpperCase())) {
                plugin.actionPerformed(response);
                return true;
            }
        }
        return false;
    }
}
