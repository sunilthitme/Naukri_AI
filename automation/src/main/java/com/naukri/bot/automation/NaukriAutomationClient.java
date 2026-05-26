package com.naukri.bot.automation;

import com.naukri.bot.automation.model.AutomationRunRequest;
import com.naukri.bot.automation.model.AutomationRunResult;
import com.naukri.bot.automation.model.LoginTestResult;

public interface NaukriAutomationClient {
    AutomationRunResult run(AutomationRunRequest request, QuestionAnswerProvider questionAnswerProvider, AutomationControl control);

    LoginTestResult testLogin(AutomationRunRequest request);
}
