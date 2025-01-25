package io.improt.vai.llm;

import io.improt.vai.llm.providers.*;

import java.util.HashMap;
import java.util.Map;

public class LLMRegistry {
    private final Map<String, IModelProvider> providers = new HashMap<>();
    private final Map<String, String> modelProviderMap = new HashMap<>();

    public void registerProvider(String providerName, IModelProvider provider) {
        providers.put(providerName, provider);
    }

    public void registerModel(String modelName, String providerName) {
        if (!providers.containsKey(providerName)) {
            throw new IllegalArgumentException("Provider '" + providerName + "' not registered.");
        }
        modelProviderMap.put(modelName, providerName);
    }

    public IModelProvider getProviderForModel(String modelName) {
        String providerName = modelProviderMap.get(modelName);
        if (providerName == null) {
            return null;
        }
        return providers.get(providerName);
    }

    public void initializeProviders() {
        for (IModelProvider provider : providers.values()) {
            provider.init();
        }
    }

    public void registerProviders() {
        registerProvider("openai-commons", new O1Provider()); // For O1
        registerProvider("openai-preview-commons", new O1PreviewProvider()); // For O1-Preview
        registerProvider("openai-mini-commons", new O1MiniProvider()); // For O1-Mini
        registerProvider("deepseek", new DeepSeekProvider()); // For DeepSeek
        registerProvider("gemini", new GeminiProvider());
    }

    public void registerModels() {
        registerModel("o1", "openai-commons");
        registerModel("o1-preview", "openai-preview-commons");
        registerModel("o1-mini", "openai-mini-commons");
        registerModel("DeepSeek", "deepseek");
        registerModel("gemini-2.0-flash-thinking-exp-01-21", "gemini");
    }
}
