import time
import random
import requests
from datetime import datetime

BASE_URL = "http://localhost:8080"

USERNAME = "admin"
PASSWORD = "123456"

# 持续保持在线的设备
ONLINE_DEVICES = [
    "CP-0001",
    "CP-0002",
    "CP-0003",
    "CP-0004",
    "CP-0005",
]

# 设备 7 只上报一次，然后停止，用来测试离线检测
OFFLINE_TEST_DEVICE = "CP-0007"


def now_str():
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def login():
    url = f"{BASE_URL}/api/auth/login"
    body = {
        "username": USERNAME,
        "password": PASSWORD
    }

    resp = requests.post(url, json=body, timeout=5)
    resp.raise_for_status()

    data = resp.json()
    if data.get("code") != 200:
        raise RuntimeError(f"登录失败：{data}")

    token = data["data"]["token"]
    token_name = data["data"].get("tokenName", "satoken")

    print(f"登录成功，tokenName={token_name}")
    return token_name, token


def build_report_body(device_code):
    return {
        "deviceCode": device_code,
        "voltage": round(random.uniform(215, 230), 2),
        "currentValue": round(random.uniform(20, 35), 2),
        "power": round(random.uniform(6, 9), 2),
        "temperature": round(random.uniform(35, 55), 2),
        "soc": round(random.uniform(40, 90), 2),
        "networkDelay": random.randint(50, 120),
        "faultCode": "NORMAL",
        "reportTime": now_str()
    }


def report_device(device_code, headers):
    url = f"{BASE_URL}/api/device/data/report"
    body = build_report_body(device_code)

    resp = requests.post(url, json=body, headers=headers, timeout=5)
    try:
        data = resp.json()
    except Exception:
        print(f"[{now_str()}] {device_code} 上报失败，HTTP={resp.status_code}, text={resp.text}")
        return

    if data.get("code") == 200:
        print(f"[{now_str()}] {device_code} 上报成功")
    else:
        print(f"[{now_str()}] {device_code} 上报失败：{data}")


def main():
    token_name, token = login()

    headers = {
        token_name: token,
        "Content-Type": "application/json"
    }

    print("\n先让 CP-0007 上报一次，之后不再上报，用于测试离线检测")
    report_device(OFFLINE_TEST_DEVICE, headers)

    print("\n开始持续模拟 CP-0001 ~ CP-0005 心跳上报")
    print("按 Ctrl + C 停止模拟器\n")

    while True:
        for device_code in ONLINE_DEVICES:
            report_device(device_code, headers)

        # 建议小于 offline-seconds。
        # 如果 offline-seconds = 30，这里 10 秒上报一次就很稳。
        time.sleep(10)


if __name__ == "__main__":
    main()