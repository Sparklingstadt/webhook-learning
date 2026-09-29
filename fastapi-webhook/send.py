"""外部サービスの代わりに通知する、Python標準ライブラリの送信例。"""

import json
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def main():
    event = {
        "id": "evt_001",
        "type": "order.created",
        "data": {"orderId": "order_001"},
    }
    request = Request(
        "http://127.0.0.1:3000/webhooks/orders",
        data=json.dumps(event).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urlopen(request, timeout=5) as response:
            print("HTTPステータス:", response.status)
            print("受信側からの返事:", response.read().decode("utf-8"))
    except HTTPError as error:
        print(f"送信失敗: HTTP {error.code}", error.read().decode("utf-8"), file=sys.stderr)
        return 1
    except URLError:
        print("接続できません。別のターミナルでサーバーを起動してください。", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
