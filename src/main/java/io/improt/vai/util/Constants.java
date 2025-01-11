package io.improt.vai.util;

import java.nio.file.Paths;

public class Constants {
    public static final String API_KEY_PATH = Paths.get(System.getProperty("user.home"), "openai-api-key.dat").toString();
    public static final String VAI_HOME_DIR = Paths.get(System.getProperty("user.home"), ".vai").toString();
    public static final String PROMPT_TEMPLATE_FILE = "data/prompt.template";
    public static final String WORKSPACES_FILE = Paths.get(VAI_HOME_DIR, "workspaces.json").toString();
    public static final String LAST_WORKSPACE_FILE = Paths.get(VAI_HOME_DIR, "last-workspace.dat").toString();
    public static final String LAST_INCREMENTAL_BACKUP_NUMBER_FILE = "vai-last-incremental-backup-number.dat";
    public static final String VAIIGNORE_FILE = ".vaiignore";
    public static final String VAI_BACKUP_DIR = "__VaiBackup";
    public static final String ENABLED_FILES_FILE = "vai_enabled_files.json";

    public static final String DEFAULT_PROMPT_TEMPLATE_B64 = "R09BTDogVGFrZSB0aGUgZm9sbG93aW5nIHJlcXVlc3QsIGFuZCBtYWtlIHJlbGV2YW50IGNoYW5nZXMgdG8gYW55IHJlbGV2YW50IGZpbGVzIG9mIHRoZSBzdXBwbGllZCBmaWxlIGNvbnRleHQ6CgpSRVFVRVNUOiA8UkVQTEFDRU1FX1dJVEhfUkVRVUVTVD4KCllvdSBNVVNUIHJlc3BvbmQgd2l0aCB0aGUgZm9sbG93aW5nIGZvcm1hdCBmb3IgYWxsIGNoYW5nZWQgZmlsZXMgKGpzb24sIHdpdGggdGhlIG1hZ2ljKQoKYGBganNvbgpNQUdJQ19KU09OX1NUQVJUClsKewogICAgImZpbGVOYW1lIjogImZ1bGwgcGF0aCBmb3VuZCBpbiA9PSA8cGF0aD4gPT0iLAogICAgIm5ld19jb250ZW50cyI6ICI8bmV3IGNvbnRlbnRzIG9mIGZpbGU+Igp9LAp7CiAgICAiZmlsZU5hbWUiOiAiYW5vdGhlciBwYXRoIiwKICAgICJuZXdfY29udGVudHMiOiAiPG1vcmUgY29udGVudHM+Igp9LAouLi4gZXRjCl0KYGBgCgpNQUdJQ19KU09OX1NUQVJUIGlzIGEgdG9rZW4gdG8gZGVub3RlIHdoZXJlIHlvdXIganNvbiByZXNwb25zZSB3aWxsIGJlZ2luLgpJZiB5b3Ugc3VwcGx5IGEgZmlsZU5hbWUgdGhhdCBkb2VzIG5vdCBleGlzdCwgaXQgd2lsbCBiZSBjcmVhdGVkLgoKVGhpbmsgYWJvdXQgYW55IHJlcXVpcmVkIGNoYW5nZXMsIGluZmVyIGluIGFyZWFzIHdoZXJlYXMgdGhlIGNvbnRleHQgbWF5IG5vdCBiZSBzdXBwbGllZC4KSWYgYSByZXF1ZXN0IGlzIGRlZW1lZCBpbXBvc3NpYmxlLCB5b3UgbWF5IGNvbW11bmljYXRlIGEgcmVwbHkgdmlhIGFub3RoZXIgbWFnaWMgIk1BR0lDX01FU1NBR0VfU1RBUlQiIHdpdGggYSBzaW1wbGUgc3RyaW5nLgoKaS5lLgpNQUdJQ19NRVNTQUdFX1NUQVJUICJTb3JyeSwgYnV0IHlvdXIgcmVxdWVzdCBpcyBpbXBvc3NpYmxlLCBmb3IgdGhlIGZvbGxvd2luZyByZWFzb246IC4uLiIKCkRvIG5vdCBpbmNsdWRlIG11bHRpcGxlIG1hZ2ljcyBwZXIgcmVzcG9uc2UsIHBpY2sgTUVTU0FHRSBvciBKU09OLgoKLS0tLS0KCkZpbGUgU3RydWN0dXJlOgo8UkVQTEFDRU1FX1dJVEhfU1RSVUNUVVJFPgoKRmlsZXMgKHByZWZpeGVkIHdpdGggPT0gPEZ1bGxSZWxhdGl2ZVBhdGg+ID09LCBhbmQgYGBgcykKPFJFUExBQ0VNRV9XSVRIX0ZJTEVTPgoKLS0tLS0KCkJlZ2luIG5vdywgZW5zdXJpbmcgY29ycmVjdG5lc3MsIGdvb2QgZm9ybWF0dGluZywgYW5kIGFkZXF1YXRlIGNvZGluZyBwcmFjdGljZXMu";
   
    public static final String RECENT_PROJECTS_FILE = Paths.get(VAI_HOME_DIR, "recent_projects.json").toString();

    public static final String RECENTLY_ACTIVE_FILES = "recentlyActive.json";
    // Add other path constants as needed
}
