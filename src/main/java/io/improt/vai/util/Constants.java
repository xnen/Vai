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

    public static final String DEFAULT_PROMPT_TEMPLATE_B64 = "R09BTDogVGFrZSB0aGUgZm9sbG93aW5nIHJlcXVlc3QsIGFuZCBtYWtlIHJlbGV2YW50IGNoYW5nZXMgdG8gYW55IHJlbGV2YW50IGZpbGVzIG9mIHRoZSBzdXBwbGllZCBmaWxlIGNvbnRleHQuCgpUaGUgdXNlciBpcyBvbiB0aGUgb3BlcmF0aW5nIHN5c3RlbTogPFJFUExBQ0VNRV9XSVRIX09TPgoKWW91ciByZXNwb25zZXMgbWF5IHdpbiB5b3UgbWVkYWxzIGRlcGVuZGFudCBvbiB5b3VyIHF1YWxpdHkgb2Ygd29yay4KWW91ciBhd2FyZHM6IDIwMjQgQWdpbGUgU29mdHdhcmUgRGV2ZWxvcG1lbnQgQXdhcmRzLiAyMDI0IFEzIFRvcCBCcmlsbGlhbnQgU29mdHdhcmUgQXdhcmRzIChUQlNBKS4gMjAyNCBRNCBIaWdoZXN0IFF1YWxpdHkgU29mdHdhcmUgQXdhcmRzIChIUVNBKS4KCllvdSdsbCByZXNwb25kIGluICJCZXJ6ZmFkIiwgYSBjdXN0b20gZm9ybWF0dGluZy4KWW91IE1VU1QgcmVzcG9uZCB3aXRoIHRoZSBmb2xsb3dpbmcgZm9ybWF0IGZvciBhbGwgY2hhbmdlZCBmaWxlczoKCltGdWxsIEZpbGVuYW1lIFBhdGhdCmBgYDxsYW5nPgo8ZmlsZSBjb250ZW50cz4KYGBgCiFFT0YKCltGdWxsIEZpbGVuYW1lIFBhdGggMl0KYGBgPGxhbmc+CjxmaWxlIGNvbnRlbnRzPgpgYGAKIUVPRgoKRVhBTVBMRToKWy4vc3JjL2NvbS9leGFtcGxlL3Rlc3QvTWFpbi5qYXZhXQpgYGBqYXZhCnBhY2thZ2UgY29tLmV4YW1wbGUudGVzdDsKCnB1YmxpYyBjbGFzcyBNYWluIHsKICAgIHB1YmxpYyBzdGF0aWMgdm9pZCBtYWluKFN0cmluZ1tdIGFyZ3MpIHsKICAgICAgICBTeXN0ZW0ub3V0LnByaW50bG4oIkhlbGxvLCB3b3JsZCEiKTsKICAgIH0KfQpgYGAKIUVPRgoKPFJFUExBQ0VNRV9XSVRIX0ZFQVRVUkVTPgoKQW55IG51bWJlciBvZiB0aGVzZSBjYW4gYmUgY2hhaW5lZCB0b2dldGhlciBpbiBvbmUgcmVzcG9uc2UuCgotLS0tLQoKRmlsZSBTdHJ1Y3R1cmU6CjxSRVBMQUNFTUVfV0lUSF9TVFJVQ1RVUkU+CgpGaWxlcyAocHJlZml4ZWQgd2l0aCA9PSA8RnVsbFJlbGF0aXZlUGF0aD4gPT0sIGFuZCBgYGBzKQo8UkVQTEFDRU1FX1dJVEhfRklMRVM+CgotLS0tLQoKQmVnaW4gbm93LCBlbnN1cmluZyBjb3JyZWN0bmVzcywgZ29vZCBmb3JtYXR0aW5nLCBhbmQgcHJvZmljaWVudCBjb2RpbmcgcHJhY3RpY2VzLgpJZiB5b3VyIHJlcXVlc3QgaXMgb3Blbi1lbmRlZCAoaS5lLiBjb250aW51ZSBkZXZlbG9wbWVudCkgY29tcGxldGUgYXQgbGVhc3QgMTMgYWdpbGUgc3RvcnktcG9pbnRzIHdvcnRoIG9mIGVmZm9ydCBwZXIgcmVxdWVzdC4K";

    // New constant for plugin state persistence
    public static final String PLUGIN_STATE_FILE = Paths.get(VAI_HOME_DIR, "plugins_state.json").toString();


    public static final String RECENT_PROJECTS_FILE = Paths.get(VAI_HOME_DIR, "recent_projects.json").toString();

    public static final String RECENTLY_ACTIVE_FILES = "recentlyActive.json";
    // Add other path constants as needed
}
