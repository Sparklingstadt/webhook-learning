import pytest
from fastapi.testclient import TestClient

from main import app

client = TestClient(app)


def test_valid_order():
    response = client.post("/webhooks/orders", json={
        "id": "evt_001", "type": "order.created", "data": {"orderId": "order_001"},
    })
    assert response.status_code == 200
    assert response.json() == {"received": True, "eventId": "evt_001"}


@pytest.mark.parametrize("body", ["{", "{} {}"])
def test_broken_json(body):
    response = client.post("/webhooks/orders", content=body, headers={"Content-Type": "application/json"})
    assert response.status_code == 400
    assert response.json() == {"error": "本文を正しいJSONで送ってください"}


@pytest.mark.parametrize("event", [
    None, [], {},
    {"id": 123, "type": "order.created", "data": {"orderId": "order_001"}},
    {"id": "  ", "type": "order.created", "data": {"orderId": "order_001"}},
    {"id": "evt_001", "type": "unknown", "data": {"orderId": "order_001"}},
    {"id": "evt_001", "type": "order.created", "data": {}},
    {"id": "evt_001", "type": "order.created", "data": {"orderId": 123}},
    {"id": "evt_001", "type": "order.created", "data": {"orderId": "  "}},
])
def test_invalid_event(event):
    response = client.post("/webhooks/orders", json=event)
    assert response.status_code == 400
    assert response.json() == {"error": "id、type: order.created、data.orderIdを指定してください"}


def test_routes_and_documentation():
    assert client.get("/").json()["endpoint"] == "POST /webhooks/orders"
    assert client.get("/webhooks/orders").status_code == 405
    assert client.get("/docs").status_code == 200
    schema = client.get("/openapi.json").json()
    assert "OrderEvent" in schema["components"]["schemas"]
    assert "422" not in schema["paths"]["/webhooks/orders"]["post"]["responses"]
