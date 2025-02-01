package io.improt.vai.backend.plugin.impl;

import io.improt.vai.backend.plugin.AbstractPlugin;

public class RequestPlanPlugin extends AbstractPlugin {
    @Override
    protected String getIdentifier() {
        return "[request plan]"; // not applicable, but used for saving states.
    }

    @Override
    protected String getExtension() {
        return "request"; // not applicable.
    }

    @Override
    protected void actionPerformed(String actionBody) {
        // nothing.
    }

    @Override
    public String getFeaturePrompt() {
        return "For complex user requests (requests that contain over 13 agile story points worth of effort), create a new document outlining a plan for the request. Name the file `RequestPlan-<request>.md`. Ensure the plan has clear milestones for completing the request.";
    }

    @Override
    public String getFeatureDescription() {
        return "Tell LLM to write plans for complex requests (improves multistep coherence).";
    }
}
