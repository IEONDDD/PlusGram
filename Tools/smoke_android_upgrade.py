"""Upgrade Plusgram 5.0.0 and check diagnostics on a dedicated Android 13+ emulator."""

import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


PACKAGE = "com.plusgram.app"
NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
UI_DUMP = "/data/local/tmp/plusgram-smoke.xml"


def smoke(adb, previous, candidate):
    def run(*args):
        return subprocess.check_output([*adb, *map(str, args)], encoding="utf-8", timeout=90)

    def shell(*args):
        return run("shell", *args)

    def launch():
        shell("am", "force-stop", PACKAGE)
        shell("monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")

    def package_state():
        state = shell("dumpsys", "package", PACKAGE)
        match = re.search(r"\bversionCode=(\d+)", state)
        return int(match[1]) if match else 0, f"{NOTIFICATIONS}: granted=true" in state

    def hierarchy():
        if "dumped to:" not in shell("uiautomator", "dump", UI_DUMP):
            return []
        return list(ET.fromstring(shell("cat", UI_DUMP)).iter("node"))

    def tap(node):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
        shell("input", "tap", (x1 + x2) // 2, (y1 + y2) // 2)

    def wait_for(text, *, onboarding=False):
        deadline = time.monotonic() + 60
        visible = {}
        while time.monotonic() < deadline:
            visible = {}
            for node in hierarchy():
                for attr in ("text", "content-desc", "resource-id"):
                    value = node.get(attr)
                    if value:
                        visible.setdefault(value, node)
            if text in visible:
                return visible[text]
            if onboarding:
                actions = (
                    "Start Messaging",
                    "Continue",
                    "com.android.permissioncontroller:id/permission_deny_button",
                    "com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button",
                )
                for action in actions:
                    if action in visible:
                        tap(visible[action])
                        break
            time.sleep(0.5)
        raise RuntimeError(f"{text!r} was not visible. Last screen: {', '.join(visible)}")

    if shell("getprop", "ro.kernel.qemu").strip() != "1":
        raise RuntimeError("Use a dedicated Android emulator without a signed-in Plusgram account.")
    if int(shell("getprop", "ro.build.version.sdk").strip()) < 33:
        raise RuntimeError("Use Android API 33 or later for the notification permission check.")
    if package_state()[0] not in (0, 6):
        raise RuntimeError("The emulator must be empty or have Plusgram 5.0.0 installed.")

    print(run("install", "-r", previous).strip(), flush=True)
    if package_state()[0] != 6:
        raise RuntimeError("The baseline APK must be Plusgram 5.0.0 (version code 6).")
    shell("pm", "grant", PACKAGE, NOTIFICATIONS)
    launch()
    wait_for("More options", onboarding=True)
    print("Plusgram 5.0.0 launched.", flush=True)

    print(run("install", "-r", candidate).strip(), flush=True)
    version, granted = package_state()
    if version <= 7 or not granted:
        raise RuntimeError("Upgrade must advance versionCode beyond 7 and preserve the notification permission.")
    launch()
    tap(wait_for("More options", onboarding=True))
    tap(wait_for("Material 3 and motion"))
    wait_for("Material 3 components")
    print("Material 3 settings opened after the upgrade.", flush=True)
    shell("input", "keyevent", "4")
    tap(wait_for("More options"))
    tap(wait_for("Notifications and connection"))
    wait_for("Telegram connection")
    wait_for("Sign in to check registration")
    wait_for("Notification permission")
    wait_for("Allowed")
    print(f"Upgraded to versionCode {version}. Diagnostics opened and the permission survived.", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("previous", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial")
    args = parser.parse_args()
    command = [args.adb, "-s", args.serial] if args.serial else [args.adb]
    smoke(command, args.previous, args.candidate)
