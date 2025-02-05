package io.improt.vai.llm.providers;

import io.improt.vai.llm.Cost;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DeepSeekProvider extends OpenAIClientBase {
    public DeepSeekProvider() {
        super(loadDeepSeekBaseUrl(),
              "hf.co/unsloth/DeepSeek-R1-Distill-Qwen-32B-GGUF:DeepSeek-R1-Distill-Qwen-32B-Q4_K_M.gguf",
              "API");
    }

    private static String loadDeepSeekBaseUrl() {
        String defaultUrl = "http://192.168.1.195:11434/v1";
        File file = new File("deepseek_url.txt");
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String url = br.readLine();
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return defaultUrl;
    }

    @Override
    public Cost getCost() {
        return Cost.FREE;
    }

    @Override
    public String getFriendlyName() {
        return "DeepSeek (ollama)";
    }
}
