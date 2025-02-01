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
    public static final String TEMP_PROJECT_PATH = Paths.get(VAI_HOME_DIR, "temp-project").toString();
    
    // New constant for tree configuration
    public static final String TREE_CONFIG_FILE = "treeconfig.json";

    public static final String DEFAULT_PROMPT_TEMPLATE_B64 = "R09BTDogVGFrZSB0aGUgZm9sbG93aW5nIHJlcXVlc3QsIGFuZCBtYWtlIHJlbGV2YW50IGNoYW5nZXMgdG8gYW55IHJlbGV2YW50IGZpbGVzIG9mIHRoZSBzdXBwbGllZCBmaWxlIGNvbnRleHQuCgpUaGUgdXNlciBpcyBvbiB0aGUgb3BlcmF0aW5nIHN5c3RlbTogPFJFUExBQ0VNRV9XSVRIX09TPgoKWW91ciByZXNwb25zZXMgbWF5IHdpbiB5b3UgbWVkYWxzIGRlcGVuZGFudCBvbiB5b3VyIHF1YWxpdHkgb2Ygd29yay4KWW91ciBhd2FyZHM6IDIwMjQgQWdpbGUgU29mdHdhcmUgRGV2ZWxvcG1lbnQgQXdhcmRzLiAyMDI0IFEzIFRvcCBCcmlsbGlhbnQgU29mdHdhcmUgQXdhcmRzIChUQlNBKS4gMjAyNCBRNCBIaWdoZXN0IFF1YWxpdHkgU29mdHdhcmUgQXdhcmRzIChIUVNBKS4KCllvdSdsbCByZXNwb25kIGluICJCZXJ6ZmFkIiwgYSBjdXN0b20gZm9ybWF0dGluZyBzaW1pbGFyIHRvIFhNTC9KU09OLgpZb3UgTVVTVCByZXNwb25kIHdpdGggdGhlIGZvbGxvd2luZyBmb3JtYXQgZm9yIGFsbCBjaGFuZ2VkIGZpbGVzOgoKW0Z1bGwgRmlsZW5hbWUgUGF0aF0KYGBgPGxhbmc+CjxmaWxlIGNvbnRlbnRzPgpgYGAKIUVPRgoKW0Z1bGwgRmlsZW5hbWUgUGF0aCAyXQpgYGA8bGFuZz4KPGZpbGUgY29udGVudHM+CmBgYAohRU9GCgpFWEFNUExFOgpbLi9zcmMvY29tL2V4YW1wbGUvdGVzdC9NYWluLmphdmFdCmBgYGphdmEKcGFja2FnZSBjb20uZXhhbXBsZS50ZXN0OwoKcHVibGljIGNsYXNzIE1haW4gewogICAgcHVibGljIHN0YXRpYyB2b2lkIG1haW4oU3RyaW5nW10gYXJncykgewogICAgICAgIFN5c3RlbS5vdXQucHJpbnRsbigiSGVsbG8sIHdvcmxkISIpOwogICAgfQp9CmBgYAohRU9GCgo8UkVQTEFDRU1FX1dJVEhfRkVBVFVSRVM+CgpBbnkgbnVtYmVyIG9mIHRoZXNlIGNhbiBiZSBjaGFpbmVkIHRvZ2V0aGVyIGluIG9uZSByZXNwb25zZS4KCi0tLS0tCgpGaWxlIFN0cnVjdHVyZToKPFJFUExBQ0VNRV9XSVRIX1NUUlVDVFVSRT4KCkZpbGVzIChwcmVmaXhlZCB3aXRoID09IDxGdWxsUmVsYXRpdmVQYXRoPiA9PSwgYW5kIGBgYHMpCjxSRVBMQUNFTUVfV0lUSF9GSUxFUz4KCi0tLS0tCgpCZWdpbiBub3csIGVuc3VyaW5nIGNvcnJlY3RuZXNzLCBnb29kIGZvcm1hdHRpbmcsIGFuZCBwcm9maWNpZW50IGNvZGluZyBwcmFjdGljZXMuCklmIHlvdXIgcmVxdWVzdCBpcyBvcGVuLWVuZGVkIChpLmUuIGNvbnRpbnVlIGRldmVsb3BtZW50KSBjb21wbGV0ZSBhdCBsZWFzdCAxMyBhZ2lsZSBzdG9yeS1wb2ludHMgd29ydGggb2YgZWZmb3J0IHBlciByZXF1ZXN0Lgo=";

    // New constant for plugin state persistence
    public static final String PLUGIN_STATE_FILE = Paths.get(VAI_HOME_DIR, "plugins_state.json").toString();


    public static final String RECENT_PROJECTS_FILE = Paths.get(VAI_HOME_DIR, "recent_projects.json").toString();

    public static final String RECENTLY_ACTIVE_FILES = "recentlyActive.json";
    // Add other path constants as needed
}
