package com.naukri.bot.automation.model;

public record ProxySettings(String host, Integer port, String username, String password) {
    public boolean enabled() {
        return host != null && !host.isBlank() && port != null && port > 0;
    }

    public String server() {
        return enabled() ? "http://" + host + ":" + port : null;
    }
}
