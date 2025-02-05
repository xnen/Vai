package io.improt.vai.llm.providers;

import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class DeepSeekProvider extends OpenAIClientBase {
    public DeepSeekProvider() {
        super("http://192.168.1.195:11434/v1",
           "hf.co/unsloth/DeepSeek-R1-Distill-Qwen-32B-GGUF:DeepSeek-R1-Distill-Qwen-32B-Q4_K_M.gguf",
               "API");
    }

    @Override
    public String getFriendlyName() {
        return "DeepSeek (ollama)";
    }
}
