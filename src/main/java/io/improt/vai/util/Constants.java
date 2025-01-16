package io.improt.vai.util;

import java.nio.file.Paths;

public class Constants {
    public static final String API_KEY_PATH = Paths.get(System.getProperty("user.home"), "openai-api-key.dat").toString();
    public static final String VAI_HOME_DIR = Paths.get(System.getProperty("user.home"), ".vai").toString();
    public static final String PROMPT_TEMPLATE_FILE = "data/prompt.template";
    public static final String WORKSPACES_FILE = Paths.get(VAI_HOME_DIR, "workspaces.json").toString();
    public static final String LAST_WORKSPACE_FILE = Paths.get(VAI_HOME_DIR, "last-workspace.dat").toString();
    public static final String LAST_INCREMENTAL_BACKUP_NUMBER_FILE = "vai-last-incremental-backup-number.dat";
    public static final String VAI_BACKUP_DIR = "__VaiBackup";
    public static final String ENABLED_FILES_FILE = "vai_enabled_files.json";
    
    // New constant for tree configuration
    public static final String TREE_CONFIG_FILE = "treeconfig.json";

    public static final String DEFAULT_PROMPT_TEMPLATE_B64 = "R09BTDogVGFrZSB0aGUgZm9sbG93aW5nIHJlcXVlc3QsIGFuZCBtYWtlIHJlbGV2YW50IGNoYW5nZXMgdG8gYW55IHJlbGV2YW50IGZpbGVzIG9mIHRoZSBzdXBwbGllZCBmaWxlIGNvbnRleHQ6ClJFUVVFU1Q6IDxSRVBMQUNFTUVfV0lUSF9SRVFVRVNUPgoKWW91J2xsIHJlc3BvbmQgaW4gIkJlcnpmYWQiLCBhIGN1c3RvbSBmb3JtYXR0aW5nIHNpbWlsYXIgdG8gWE1ML0pTT04uCllvdSBNVVNUIHJlc3BvbmQgd2l0aCB0aGUgZm9sbG93aW5nIGZvcm1hdCBmb3IgYWxsIGNoYW5nZWQgZmlsZXM6CgpbRnVsbCBGaWxlbmFtZSBQYXRoXQpgYGA8bGFuZz4KPGZpbGUgY29udGVudHM+CmBgYAohRU9GCgpbRnVsbCBGaWxlbmFtZSBQYXRoIDJdCmBgYDxsYW5nPgo8ZmlsZSBjb250ZW50cz4KYGBgCiFFT0YKCkVYQU1QTEU6ClsuL3NyYy9pby9pbXByb3QvdGVzdC9NYWluLmphdmFdCmBgYGphdmEKcGFja2FnZSBpby5pbXByb3QudGVzdDsKCnB1YmxpYyBjbGFzcyBNYWluIHsKICAgIHB1YmxpYyBzdGF0aWMgdm9pZCBtYWluKFN0cmluZ1tdIGFyZ3MpIHsKICAgICAgICBTeXN0ZW0ub3V0LnByaW50bG4oIkhlbGxvLCB3b3JsZCEiKTsKICAgIH0KfQpgYGAKIUVPRgoKWW91IGNhbiBhbHNvIHNlbmQgbWVzc2FnZSByZXNwb25zZXMsIGJ5IHV0aWxpemluZyB0aGUgcGF0aCBbU0hPV19NRVNTQUdFXSByYXRoZXIgdGhhbiBhIGZpbGUgcGF0aCwgd2l0aCB0aGUgYGNoYXRgIGxhbmcuCgpFWEFNUExFOgpbU0hPV19NRVNTQUdFXQpgYGBjaGF0CllvdXIgcmVxdWVzdCBpcyBpbXBvc3NpYmxlLCBmb3IgdGhlIGZvbGxvd2luZyByZWFzb246IDxyZWFzb24+CmBgYAohRU9GCgpZb3UgY2FuIHN1Z2dlc3QgYSBzaGVsbCBjb21tYW5kIHRvIHJ1biwgYnkgdXRpbGl6aW5nIHRoZSBwYXRoIFtSVU5fQ09NTUFORF0gcmF0aGVyIHRoYW4gYSBmaWxlIHBhdGgsIHdpdGggdGhlIGBydW5gIGxhbmcuCkl0IHdpbGwgcHJvbXB0IHRoZSB1c2VyIHRvIHJ1biB0aGUgY29tbWFuZCwgYW5kIHRoZW4gcmVzcG9uZCB3aXRoIHRoZSBvdXRwdXQuCgpFWEFNUExFOgpbUlVOX0NPTU1BTkRdCmBgYHJ1bgplY2hvICJIZWxsbywgd29ybGQhIgpgYGAKIUVPRgoKVGhlIHVzZXIgaXMgb24gdGhlIG9wZXJhdGluZyBzeXN0ZW06IDxSRVBMQUNFTUVfV0lUSF9PUz4KCllvdSBjYW4gc3VnZ2VzdCB0aGUgcHJvbXB0IHRvIHVzZSBmb3IgdGhlIExMTSwgYnkgdXRpbGl6aW5nIHRoZSBwYXRoIFtMTE1fUFJPTVBUXSByYXRoZXIgdGhhbiBhIGZpbGUgcGF0aCwgd2l0aCB0aGUgYHByb21wdGAgbGFuZy4KVGhpcyB3aWxsIGJlIHRoZSBuZXh0IHByb21wdCBzZW50IHRvIHRoZSBMTE0uCgpFeGFtcGxlOgpbTExNX1BST01QVF0KYGBgcHJvbXB0Ci4uLkxldCdzIGNvbnRpbnVlIGJ5IGltcGxlbWVudGluZyB0aGUgaW50ZXJmYWNlIHdlIGp1c3QgY3JlYXRlZC4KYGBgCiFFT0YKCkFueSBudW1iZXIgb2YgdGhlc2UgY2FuIGJlIGNoYWluZWQgdG9nZXRoZXIgaW4gb25lIHJlc3BvbnNlLgoKLS0tLS0KCkZpbGUgU3RydWN0dXJlOgo8UkVQTEFDRU1FX1dJVEhfU1RSVUNUVVJFPgoKRmlsZXMgKHByZWZpeGVkIHdpdGggPT0gPEZ1bGxSZWxhdGl2ZVBhdGg+ID09LCBhbmQgYGBgcykKPFJFUExBQ0VNRV9XSVRIX0ZJTEVTPgoKLS0tLS0KCkJlZ2luIG5vdywgZW5zdXJpbmcgY29ycmVjdG5lc3MsIGdvb2QgZm9ybWF0dGluZywgYW5kIGFkZXF1YXRlIGNvZGluZyBwcmFjdGljZXMuIEZvbGxvdyBhbGwgY29kaW5nIGd1aWRlbGluZXMgaWYgc3BlY2lmaWVkLgpUaGUgcHJvamVjdCBpcyBOT1QgYSBnYW1lLCB1bmxlc3Mgc3BlY2lmaWVkIG90aGVyd2lzZS4KQSBkZXYgbWF5IG5vdGUgdGhpbmdzIHRoYXQgeW91IHNob3VsZCBhdHRlbmQgdG8sIHVzaW5nICJHUFRPRE8iIGluIHRoZSBjb2RlLiBUZW5kIHRoZXNlIGFzIHlvdSBnby4KWW91J3JlIGVuY291cmFnZWQgdG8gcHJvdmlkZSBuZXh0IHByb21wdHMgdXNpbmcgTExNX1BST01QVC4=";

    // Old prompt template
//    public static final String DEFAULT_PROMPT_TEMPLATE_B64 = "R09BTDogVGFrZSB0aGUgZm9sbG93aW5nIHJlcXVlc3QsIGFuZCBtYWtlIHJlbGV2YW50IGNoYW5nZXMgdG8gYW55IHJlbGV2YW50IGZpbGVzIG9mIHRoZSBzdXBwbGllZCBmaWxlIGNvbnRleHQ6CgpSRVFVRVNUOiA8UkVQTEFDRU1FX1dJVEhfUkVRVUVTVD4KCllvdSBNVVNUIHJlc3BvbmQgd2l0aCB0aGUgZm9sbG93aW5nIGZvcm1hdCBmb3IgYWxsIGNoYW5nZWQgZmlsZXMgKGpzb24sIHdpdGggdGhlIG1hZ2ljKQoKYGBganNvbgpNQUdJQ19KU09OX1NUQVJUClsKewogICAgImZpbGVOYW1lIjogImZ1bGwgcGF0aCBmb3VuZCBpbiA9PSA8cGF0aD4gPT0iLAogICAgIm5ld19jb250ZW50cyI6ICI8bmV3IGNvbnRlbnRzIG9mIGZpbGU+Igp9LAp7CiAgICAiZmlsZU5hbWUiOiAiYW5vdGhlciBwYXRoIiwKICAgICJuZXdfY29udGVudHMiOiAiPG1vcmUgY29udGVudHM+Igp9LAouLi4gZXRjCl0KYGBgCgpNQUdJQ19KU09OX1NUQVJUIGlzIGEgdG9rZW4gdG8gZGVub3RlIHdoZXJlIHlvdXIganNvbiByZXNwb25zZSB3aWxsIGJlZ2luLgpJZiB5b3Ugc3VwcGx5IGEgZmlsZU5hbWUgdGhhdCBkb2VzIG5vdCBleGlzdCwgaXQgd2lsbCBiZSBjcmVhdGVkLgoKVGhpbmsgYWJvdXQgYW55IHJlcXVpcmVkIGNoYW5nZXMsIGluZmVyIGluIGFyZWFzIHdoZXJlYXMgdGhlIGNvbnRleHQgbWF5IG5vdCBiZSBzdXBwbGllZC4KSWYgYSByZXF1ZXN0IGlzIGRlZW1lZCBpbXBvc3NpYmxlLCB5b3UgbWF5IGNvbW11bmljYXRlIGEgcmVwbHkgdmlhIGFub3RoZXIgbWFnaWMgIk1BR0lDX01FU1NBR0VfU1RBUlQiIHdpdGggYSBzaW1wbGUgc3RyaW5nLgoKaS5lLgpNQUdJQ19NRVNTQUdFX1NUQVJUICJTb3JyeSwgYnV0IHlvdXIgcmVxdWVzdCBpcyBpbXBvc3NpYmxlLCBmb3IgdGhlIGZvbGxvd2luZyByZWFzb246IC4uLiIKCkRvIG5vdCBpbmNsdWRlIG11bHRpcGxlIG1hZ2ljcyBwZXIgcmVzcG9uc2UsIHBpY2sgTUVTU0FHRSBvciBKU09OLgoKLS0tLS0KCkZpbGUgU3RydWN0dXJlOgo8UkVQTEFDRU1FX1dJVEhfU1RSVUNUVVJFPgoKRmlsZXMgKHByZWZpeGVkIHdpdGggPT0gPEZ1bGxSZWxhdGl2ZVBhdGg+ID09LCBhbmQgYGBgcykKPFJFUExBQ0VNRV9XSVRIX0ZJTEVTPgoKLS0tLS0KCkJlZ2luIG5vdywgZW5zdXJpbmcgY29ycmVjdG5lc3MsIGdvb2QgZm9ybWF0dGluZywgYW5kIGFkZXF1YXRlIGNvZGluZyBwcmFjdGljZXMu";
   
    public static final String RECENT_PROJECTS_FILE = Paths.get(VAI_HOME_DIR, "recent_projects.json").toString();

    public static final String RECENTLY_ACTIVE_FILES = "recentlyActive.json";
    // Add other path constants as needed
}
