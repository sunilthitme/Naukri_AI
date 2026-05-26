package com.naukri.bot.automation;

import com.naukri.bot.automation.model.AutomationRunRequest;
import com.naukri.bot.automation.model.AutomationRunResult;

public interface NaukriAutomationClient {
    AutomationRunResult run(AutomationRunRequest request, QuestionAnswerProvider questionAnswerProvider, AutomationControl control);

    boolean testLogin(AutomationRunRequest request);
}
