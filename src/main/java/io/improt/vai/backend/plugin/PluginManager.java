package io.improt.vai.backend.plugin;

import io.improt.vai.backend.plugin.impl.LLMPromptPlugin;
import io.improt.vai.backend.plugin.impl.RunCommandPlugin;
import io.improt.vai.backend.plugin.impl.ShowMessagePlugin;

import java.util.List;

public class PluginManager {
    private final List<AbstractPlugin> pluginList;

    public PluginManager() {
        pluginList = List.of(
                new ShowMessagePlugin(),
                new RunCommandPlugin(),
                new LLMPromptPlugin()
        );
    }

    public boolean passResponse(String fileName, String type, String response) {
        for (AbstractPlugin plugin : pluginList) {
            if (!plugin.isActive()) {
                continue;
            }

            if (plugin.getIdentifier().equals(fileName.toUpperCase()) && plugin.getExtension().equals(type)) {
                plugin.actionPerformed(response);
                return true;
            }
        }

        return false;
    }
}
