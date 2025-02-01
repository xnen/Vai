package io.improt.vai.llm;

import io.improt.vai.llm.providers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        // Lazy initialization is used. Providers will initialize on first request.
        System.out.println("Providers will be lazily initialized on request.");
    }

    public void registerProviders() {
        registerProvider("openai-commons", new O1Provider());
        registerProvider("openai-preview-commons", new O1PreviewProvider());
        registerProvider("openai-mini-commons", new O1MiniProvider());
        registerProvider("deepseek", new DeepSeekProvider());
        registerProvider("deepseek-nv", new NVIDIADeepSeekProvider());
        registerProvider("gemini", new GeminiProvider());
        registerProvider("o3-mini", new O3MiniProvider());
    }

    public void registerModels() {
        registerModel("o1", "openai-commons");
        registerModel("o1-preview", "openai-preview-commons");
        registerModel("o1-mini", "openai-mini-commons");
        registerModel("DeepSeek (Local)", "deepseek");
        registerModel("DeepSeek (NVIDIA)", "deepseek-nv");
        registerModel("gemini-2.0-flash-thinking-exp-01-21", "gemini");
        registerModel("o3-mini", "o3-mini");
    }
    
    // NEW: Method to retrieve all registered model names.
    public Set<String> getRegisteredModelNames() {
        return modelProviderMap.keySet();
    }
}
