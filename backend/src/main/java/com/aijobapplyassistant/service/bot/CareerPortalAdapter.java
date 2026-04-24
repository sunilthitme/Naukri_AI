package com.aijobapplyassistant.service.bot;

import com.aijobapplyassistant.entity.User;
import com.microsoft.playwright.Page;

public interface CareerPortalAdapter {

    boolean supports(String url);

    PortalSubmissionResult apply(Page page, User user, JobListing listing, String resumePath);
}
