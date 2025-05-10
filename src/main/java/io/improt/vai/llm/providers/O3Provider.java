package io.improt.vai.llm.providers;

import io.improt.vai.llm.Cost;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class O3Provider extends OpenAIClientBase implements IModelProvider {

    public O3Provider() {
        super("o3");
    }

    @Override
    public boolean supportsDeveloperRole() {
        return true;
    }

    @Override
    public boolean supportsReasoningEffort() {
        return true;
    }

    @Override
    public boolean supportsVision() {
        return true;
    }

    @Override
    public Cost getCost() {
        return Cost.MEDIUM;
    }
}
