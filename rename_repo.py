# -*- coding: utf-8 -*-
import json
import subprocess
import urllib.request

OLD = "awds187339858/Firewheel"
NEW_NAME = "NETAV"
PROXY = "http://127.0.0.1:7890"


def get_token():
    p = subprocess.run(
        ["git", "credential", "fill"],
        input="protocol=https\nhost=github.com\n\n",
        capture_output=True, text=True)
    for line in (p.stdout or "").splitlines():
        if line.startswith("password="):
            return line.split("=", 1)[1]
    return None


token = get_token()
if not token:
    print("FAIL: 拿不到 token")
    raise SystemExit(1)

h = urllib.request.ProxyHandler({"http": PROXY, "https": PROXY})
op = urllib.request.build_opener(h)

payload = json.dumps({"name": NEW_NAME}).encode("utf-8")
req = urllib.request.Request(
    "https://api.github.com/repos/" + OLD,
    data=payload, method="PATCH",
    headers={"Authorization": "token " + token,
             "Accept": "application/vnd.github+json",
             "Content-Type": "application/json; charset=utf-8"})
try:
    r = json.loads(op.open(req).read())
except Exception as e:
    print("改名失败:", e)
    raise SystemExit(1)

print("full_name :", r.get("full_name"))
print("html_url  :", r.get("html_url"))
print("clone_url :", r.get("clone_url"))
