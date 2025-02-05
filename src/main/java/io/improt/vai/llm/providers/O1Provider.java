package io.improt.vai.llm.providers;

import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class O1Provider extends OpenAIClientBase implements IModelProvider {

    public O1Provider() {
        super("o1");
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
}
