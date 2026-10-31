# -*- coding: utf-8 -*-
import json
import urllib.request

REPO = "awds187339858/NETAV"
PROXY = "http://127.0.0.1:7890"

h = urllib.request.ProxyHandler({"http": PROXY, "https": PROXY})
op = urllib.request.build_opener(h)

url = "https://api.github.com/repos/%s/releases/tags/v1.0" % REPO
req = urllib.request.Request(url, headers={"Accept": "application/vnd.github+json"})
try:
    r = json.loads(op.open(req).read())
except Exception as e:
    print("查询失败:", e)
    raise SystemExit(1)

print("name      :", r.get("name"))
print("tag_name  :", r.get("tag_name"))
print("draft     :", r.get("draft"))
print("published :", r.get("published_at"))
print("html_url  :", r.get("html_url"))
assets = r.get("assets") or []
print("assets    :", len(assets))
for a in assets:
    print("  - %s  %d bytes" % (a.get("name"), a.get("size")))
    print("    " + a.get("browser_download_url", ""))

