package io.improt.vai.util;

import java.nio.file.Paths;

public class Constants {
    public static final String OAI_API_KEY_PATH = Paths.get(System.getProperty("user.home"), "openai-api-key.dat").toString();
    public static final String NV_API_KEY_PATH = Paths.get(System.getProperty("user.home"), "nvidia-api-key.dat").toString();
    public static final String VAI_HOME_DIR = Paths.get(System.getProperty("user.home"), ".vai").toString();
    public static final String PROMPT_TEMPLATE_FILE = "data/prompt.template";
    public static final String WORKSPACES_FILE = Paths.get(VAI_HOME_DIR, "workspaces.json").toString();
    public static final String LAST_WORKSPACE_FILE = Paths.get(VAI_HOME_DIR, "last-workspace.dat").toString();
    public static final String LAST_INCREMENTAL_BACKUP_NUMBER_FILE = "vai-last-incremental-backup-number.dat";
    public static final String VAI_BACKUP_DIR = "__VaiBackup";
    public static final String ENABLED_FILES_FILE = "vai_enabled_files.json";
    public static final String SUBWORKSPACE_DEFINITIONS_FILE = "subworkspace_definitions.json";
    
    // New constant for tree configuration
    public static final String TREE_CONFIG_FILE = "treeconfig.json";

    // New constant for plugin state persistence
    public static final String PLUGIN_STATE_FILE = Paths.get(VAI_HOME_DIR, "plugins_state.json").toString();


    public static final String RECENT_PROJECTS_FILE = Paths.get(VAI_HOME_DIR, "recent_projects.json").toString();

    public static final String RECENTLY_ACTIVE_FILES = "recentlyActive.json";
    // Add other path constants as needed
}
