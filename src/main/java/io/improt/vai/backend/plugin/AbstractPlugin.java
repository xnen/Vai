package io.improt.vai.backend.plugin;

public abstract class AbstractPlugin {
    private boolean active = true;

    public boolean isActive() {
        return active;
    }

    protected abstract String getIdentifier();
    protected abstract String getExtension();

    protected abstract void actionPerformed(String actionBody);
}
