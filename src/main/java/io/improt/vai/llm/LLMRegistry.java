package io.improt.vai.llm;

import io.improt.vai.llm.providers.*;
import io.improt.vai.llm.providers.impl.IModelProvider;

import java.util.*;

public class LLMRegistry {
    private final Map<String, IModelProvider> models = new HashMap<>();

    public void register(IModelProvider provider) {
        models.put(provider.getFriendlyName(), provider);
    }

    public IModelProvider getModel(String modelName) {
        return models.get(modelName);
    }

    private final List<IModelProvider> modelList = new ArrayList<>();

    public void registerModels() {
//        register(new GeminiProvider());
        register(new FourOProvider());
        register(new GPT4oSearchProvider());
        register(new GPT41Provider());
        register(new O3MiniProvider());
        register(new O3Provider());
        register(new O4MiniProvider());
        register(new O1Provider());
        register(new O1MiniProvider());
        register(new DeepSeekProvider());
        register(new O1PreviewProvider());
        register(new FourOAudioProvider());
        register(new FourOAudioMiniProvider());
        register(new NVIDIADeepSeekProvider());
        register(new ClaudeProvider());
        register(new GeminiProProvider());

        modelList.addAll(this.models.values());

        modelList.sort((p1, p2) -> {
            if (p1.getCost() == Cost.FREE && p2.getCost() != Cost.FREE) {
                return -1;
            } else if (p2.getCost() == Cost.FREE && p1.getCost() != Cost.FREE) {
                return 1;
            }
            return p2.getCost().compareTo(p1.getCost());
        });

        System.out.println("Models registered:");
        for (IModelProvider modelProvider : modelList) {
            System.out.println("- " + modelProvider.getFriendlyName());
        }
    }

    public List<String> getRegisteredModelNames() {
        List<String> a = new ArrayList<>();
        for (IModelProvider m : modelList) {
            a.add(m.getFriendlyName());
        }
        return a;
    }
}
