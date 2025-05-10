package io.improt.vai.llm.providers;

import io.improt.vai.llm.Cost;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class O4MiniProvider extends OpenAIClientBase implements IModelProvider {

    public O4MiniProvider() {
        super("o4-mini");
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
