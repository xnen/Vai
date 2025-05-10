package io.improt.vai.llm.providers;

import io.improt.vai.llm.Cost;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class GPT4oSearchProvider extends OpenAIClientBase implements IModelProvider {

    public GPT4oSearchProvider() {
        super("gpt-4o-mini-search-preview");
    }

    @Override
    public Cost getCost() {
        return Cost.HIGH;
    }

    @Override
    public boolean supportsVision() {
        return true;
    }
}
