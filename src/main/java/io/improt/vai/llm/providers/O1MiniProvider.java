package io.improt.vai.llm.providers;

import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class O1MiniProvider extends OpenAIClientBase implements IModelProvider {

    public O1MiniProvider() {
        super("o1-mini");
    }

}
