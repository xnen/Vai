package io.improt.vai.backend.plugin.impl;

import io.improt.vai.backend.App;
import io.improt.vai.backend.plugin.AbstractPlugin;


public class LLMPromptPlugin extends AbstractPlugin {
    @Override
    protected String getIdentifier() {
        return "LLM_PROMPT";
    }

    @Override
    protected String getExtension() {
        return "prompt";
    }

    @Override
    protected void actionPerformed(String actionBody) {
        App.getInstance().getClient().setLLMPrompt(actionBody);
    }
    
    @Override
    public String getFeaturePrompt() {
        return "You can suggest the prompt to use for the LLM, by utilizing the path [LLM_PROMPT] rather than a file path, with the `prompt` lang. This will be the next prompt sent to the LLM.\n" +
                "You must suggest possible next steps after each request using the LLM_PROMPT feature. Suggest at least 13 agile story points worth of effort.\n" +
                "Continue prompting as needed to continue assisting with development -- analyze any missing or incorrect pieces and implement as you go.\n"+
                "\n" +
                "Example:\n" +
                "[LLM_PROMPT]\n" +
                "```prompt\n" +
                "...Let's continue by implementing the interface we just created.\n" +
                "```\n" +
                "!EOF";
    }

    @Override
    public String getFeatureDescription() {
        return "Allow LLM to continue prompting itself (green text)";
    }
}
