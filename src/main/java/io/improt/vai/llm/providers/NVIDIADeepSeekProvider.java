package io.improt.vai.llm.providers;

import io.improt.vai.backend.App;
import io.improt.vai.llm.Cost;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

public class NVIDIADeepSeekProvider extends OpenAIClientBase {

    public NVIDIADeepSeekProvider() {
        super("https://integrate.api.nvidia.com/v1", "deepseek-ai/deepseek-r1", App.GetNvidiaKey());
    }

    @Override
    public Cost getCost() {
        return Cost.LOW; // free credits.
    }

    @Override
    public String getFriendlyName() {
        return "DeepSeek (NVIDIA)";
    }

}
