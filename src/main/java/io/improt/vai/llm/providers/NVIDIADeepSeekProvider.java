package io.improt.vai.llm.providers;

import io.improt.vai.backend.App;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class NVIDIADeepSeekProvider extends OpenAIClientBase {

    public NVIDIADeepSeekProvider() {
        super("https://integrate.api.nvidia.com/v1", "deepseek-ai/deepseek-r1", App.GetNvidiaKey());
    }
}
