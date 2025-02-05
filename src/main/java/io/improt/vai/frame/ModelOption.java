package io.improt.vai.frame;

public class ModelOption {
    public String label;
    public boolean enabled;
    public final boolean supportsAudio;
    public final boolean supportsImage;

    public ModelOption(String label, boolean supportsAudio, boolean supportsImage) {
        this.label = label;
        this.supportsAudio = supportsAudio;
        this.supportsImage = supportsImage;
        this.enabled = true;
    }

    @Override
    public String toString() {
        return label;
    }
}
