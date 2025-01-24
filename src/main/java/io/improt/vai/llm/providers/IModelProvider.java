package io.improt.vai.llm.providers;

import java.io.File;
import java.util.List;

public interface IModelProvider {
    String request(String model, String prompt, String userRequest, List<File> files);
    void init();

    boolean supportsAudio();
    boolean supportsVideo();
    boolean supportsVision();
}
