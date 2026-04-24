package com.aijobapplyassistant.service.bot;

import com.aijobapplyassistant.service.PortalCredentialService;
import com.aijobapplyassistant.service.QuestionQueueService;
import com.aijobapplyassistant.service.SystemLogService;
import com.aijobapplyassistant.service.ai.OllamaAiService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
public class TaleoCareerPortalAdapter extends GenericCareerPortalAdapter {

    public TaleoCareerPortalAdapter(OllamaAiService ollamaAiService, QuestionQueueService questionQueueService,
            SystemLogService systemLogService, PortalCredentialService portalCredentialService) {
        super(ollamaAiService, questionQueueService, systemLogService, portalCredentialService);
    }

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("taleo");
    }
}
