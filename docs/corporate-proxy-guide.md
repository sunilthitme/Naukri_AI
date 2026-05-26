# Corporate Proxy Guide

Set the proxy variables in `.env` before starting the backend:

```env
BOT_PROXY_HOST=proxy.corp.local
BOT_PROXY_PORT=8080
BOT_PROXY_USERNAME=domain-user
BOT_PROXY_PASSWORD=secret
HTTPS_PROXY=http://proxy.corp.local:8080
HTTP_PROXY=http://proxy.corp.local:8080
NO_PROXY=localhost,127.0.0.1
```

For Maven on Windows, add proxy settings to `%USERPROFILE%\.m2\settings.xml`. For npm, use `npm.cmd config set proxy` and `npm.cmd config set https-proxy` when your corporate network requires it.
