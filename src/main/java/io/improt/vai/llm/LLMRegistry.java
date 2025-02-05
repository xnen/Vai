package io.improt.vai.llm;

import io.improt.vai.llm.providers.*;
import io.improt.vai.llm.providers.impl.IModelProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LLMRegistry {
    private final Map<String, IModelProvider> models = new HashMap<>();

    public void register(IModelProvider provider) {
        models.put(provider.getFriendlyName(), provider);
    }

    public IModelProvider getModel(String modelName) {
        return models.get(modelName);
    }

    public void registerModels() {
        register(new O3MiniProvider());
        register(new O1Provider());
        register(new GeminiProvider());
        register(new O1MiniProvider());
        register(new DeepSeekProvider());
        register(new O1PreviewProvider());
        register(new FourOProvider());
        register(new FourOAudioProvider());
        register(new FourOAudioMiniProvider());
        register(new NVIDIADeepSeekProvider());
    }

    public Set<String> getRegisteredModelNames() {
        return models.keySet();
    }
}
