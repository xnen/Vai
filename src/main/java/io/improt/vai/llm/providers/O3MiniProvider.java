package io.improt.vai.llm.providers;

import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class O3MiniProvider extends OpenAIClientBase implements IModelProvider {

    public O3MiniProvider() {
        super("o3-mini");
    }

    @Override
    public boolean supportsDeveloperRole() {
        return true;
    }

    @Override
    public boolean supportsReasoningEffort() {
        return true;
    }
}
