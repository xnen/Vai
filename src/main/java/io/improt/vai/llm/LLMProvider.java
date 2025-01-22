package io.improt.vai.llm;

import java.io.File;
import java.util.List;

public interface LLMProvider {
    String request(String model, String prompt, List<File> files);
    void init();
}
