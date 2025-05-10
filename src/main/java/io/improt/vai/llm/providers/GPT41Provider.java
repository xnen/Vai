package io.improt.vai.llm.providers;

import io.improt.vai.llm.Cost;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class GPT41Provider extends OpenAIClientBase implements IModelProvider {

    public GPT41Provider() {
        super("gpt-4.1");
    }

    @Override
    public Cost getCost() {
        return Cost.MEDIUM;
    }

    @Override
    public boolean supportsVision() {
        return true;
    }
}
