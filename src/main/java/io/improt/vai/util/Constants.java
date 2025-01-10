package io.improt.vai.util;

import java.nio.file.Paths;

public class Constants {
    public static final String API_KEY_PATH = Paths.get(System.getProperty("user.home"), "openai-api-key.dat").toString();
    public static final String VAI_HOME_DIR = Paths.get(System.getProperty("user.home"), ".vai").toString();
    public static final String WORKSPACES_FILE = Paths.get(VAI_HOME_DIR, "workspaces.json").toString();
    public static final String LAST_WORKSPACE_FILE = Paths.get(VAI_HOME_DIR, "last-workspace.dat").toString();
    public static final String LAST_INCREMENTAL_BACKUP_NUMBER_FILE = "vai-last-incremental-backup-number.dat";
    public static final String VAIIGNORE_FILE = ".vaiignore";
    public static final String VAI_BACKUP_DIR = "__VaiBackup";
    public static final String ENABLED_FILES_FILE = "vai_enabled_files.json";
    // Add other path constants as needed
}
