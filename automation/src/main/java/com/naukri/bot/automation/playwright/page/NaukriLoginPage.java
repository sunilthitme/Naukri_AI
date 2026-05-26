package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.naukri.bot.automation.playwright.HumanBehavior;

public class NaukriLoginPage {
    private final Page page;
    private final HumanBehavior human = new HumanBehavior();

    public NaukriLoginPage(Page page) {
        this.page = page;
    }

    public void login(String email, String password) {
        page.navigate("https://www.naukri.com/nlogin/login",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        human.pause();
        page.locator("input[placeholder*='Email'], input[type='text'], input[name='email']").first().fill(email);
        human.pause();
        page.locator("input[type='password']").first().fill(password);
        human.pause();
        page.locator("button:has-text('Login'), button[type='submit']").first().click();
        page.waitForLoadState();
        human.pause();
    }

    public boolean captchaDetected() {
        String body = page.locator("body").innerText().toLowerCase();
        String url = page.url().toLowerCase();
        return body.contains("captcha") || body.contains("verify") || url.contains("captcha");
    }
}
