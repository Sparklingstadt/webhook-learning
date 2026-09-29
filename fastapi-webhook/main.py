"""同じ注文通知をFastAPIとPydanticで受け取る教材。"""

from typing import Literal

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, StrictStr, field_validator

app = FastAPI(title="Webhook学習サーバー")


class OrderData(BaseModel):
    orderId: StrictStr

    @field_validator("orderId")
    @classmethod
    def require_order_id(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("注文IDを指定してください")
        return value


class OrderEvent(BaseModel):
    id: StrictStr
    type: Literal["order.created"]
    data: OrderData

    @field_validator("id")
    @classmethod
    def require_event_id(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("通知IDを指定してください")
        return value


class ErrorResponse(BaseModel):
    error: str


class ReceivedResponse(BaseModel):
    received: bool
    eventId: str


# FastAPIの標準は422。この教材では他の実装と揃えて400を返します。
@app.exception_handler(RequestValidationError)
async def invalid_request(_request: Request, error: RequestValidationError):
    malformed = any(item["type"] == "json_invalid" for item in error.errors())
    message = (
        "本文を正しいJSONで送ってください"
        if malformed
        else "id、type: order.created、data.orderIdを指定してください"
    )
    return JSONResponse(status_code=400, content={"error": message})


@app.get("/")
async def index():
    return {"message": "Webhook学習サーバー", "endpoint": "POST /webhooks/orders"}


@app.post(
    "/webhooks/orders",
    response_model=ReceivedResponse,
    responses={"4XX": {"model": ErrorResponse, "description": "入力エラーは400"}},
)
async def receive_order(event: OrderEvent):
    # 型の指定によって、JSON解析と入力チェックがこの関数の前に行われます。
    print(f"[受信] event={event.id} 注文={event.data.orderId}", flush=True)
    return {"received": True, "eventId": event.id}
