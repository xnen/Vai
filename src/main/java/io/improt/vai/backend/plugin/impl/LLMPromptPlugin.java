package io.improt.vai.backend.plugin.impl;

import io.improt.vai.backend.App;
import io.improt.vai.backend.plugin.AbstractPlugin;
import io.improt.vai.util.VaiUtils;

import javax.swing.*;

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
}
