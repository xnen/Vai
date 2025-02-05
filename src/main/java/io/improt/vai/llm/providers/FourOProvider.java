package io.improt.vai.llm.providers;

import io.improt.vai.llm.Cost;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class FourOProvider extends OpenAIClientBase implements IModelProvider {

    public FourOProvider() {
        super("chatgpt-4o-latest");
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
