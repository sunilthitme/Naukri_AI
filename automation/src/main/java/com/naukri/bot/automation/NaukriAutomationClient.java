package com.naukri.bot.automation;

import com.naukri.bot.automation.model.AutomationRunRequest;
import com.naukri.bot.automation.model.AutomationRunResult;
import com.naukri.bot.automation.model.LoginTestResult;

public interface NaukriAutomationClient {
    AutomationRunResult run(AutomationRunRequest request,
                            QuestionAnswerProvider questionAnswerProvider,
                            AutomationControl control,
                            AutomationActivityListener activityListener);

    default AutomationRunResult run(AutomationRunRequest request,
                                    QuestionAnswerProvider questionAnswerProvider,
                                    AutomationControl control) {
        return run(request, questionAnswerProvider, control, AutomationActivityListener.NOOP);
    }

    LoginTestResult testLogin(AutomationRunRequest request, AutomationActivityListener activityListener);

    default LoginTestResult testLogin(AutomationRunRequest request) {
        return testLogin(request, AutomationActivityListener.NOOP);
    }
}
