package io.improt.vai.llm;

import java.util.HashMap;
import java.util.Map;

public class LLMRegistry {
    private final Map<String, LLMProvider> providers = new HashMap<>();
    private final Map<String, String> modelProviderMap = new HashMap<>();

    public void registerProvider(String providerName, LLMProvider provider) {
        providers.put(providerName, provider);
    }

    public void registerModel(String modelName, String providerName) {
        if (!providers.containsKey(providerName)) {
            throw new IllegalArgumentException("Provider '" + providerName + "' not registered.");
        }
        modelProviderMap.put(modelName, providerName);
    }

    public LLMProvider getProviderForModel(String modelName) {
        String providerName = modelProviderMap.get(modelName);
        if (providerName == null) {
            return null;
        }
        return providers.get(providerName);
    }

    public void initializeProviders() {
        for (LLMProvider provider : providers.values()) {
            provider.init();
        }
    }
}
