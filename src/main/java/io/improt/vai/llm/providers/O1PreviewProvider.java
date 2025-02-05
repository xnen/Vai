package io.improt.vai.llm.providers;

import io.improt.vai.llm.Cost;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class O1PreviewProvider extends OpenAIClientBase implements IModelProvider {
    public O1PreviewProvider() {
        super("o1-preview");
    }

    @Override
    public Cost getCost() {
        return Cost.HIGH;
    }
}
