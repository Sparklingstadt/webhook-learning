package webhook

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestReceiveOrder(t *testing.T) {
	tests := []struct {
		name   string
		body   string
		status int
	}{
		{"正常な注文", `{"id":"evt_001","type":"order.created","data":{"orderId":"order_001"}}`, 200},
		{"壊れたJSON", `{`, 400},
		{"空の本文", ``, 400},
		{"JSONが2つ続く", `{} {}`, 400},
		{"null", `null`, 400},
		{"配列", `[]`, 400},
		{"項目不足", `{}`, 400},
		{"注文ID不足", `{"id":"evt_001","type":"order.created","data":{}}`, 400},
		{"未知の種類", `{"id":"evt_001","type":"unknown","data":{"orderId":"order_001"}}`, 400},
		{"IDが数値", `{"id":123,"type":"order.created","data":{"orderId":"order_001"}}`, 400},
		{"空白のID", `{"id":"  ","type":"order.created","data":{"orderId":"order_001"}}`, 400},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			recorder := httptest.NewRecorder()
			request := httptest.NewRequest(http.MethodPost, "/webhooks/orders", strings.NewReader(tt.body))
			request.Header.Set("Content-Type", "application/json")
			NewHandler().ServeHTTP(recorder, request)
			if recorder.Code != tt.status {
				t.Fatalf("status = %d, want %d", recorder.Code, tt.status)
			}
			var result map[string]any
			if err := json.Unmarshal(recorder.Body.Bytes(), &result); err != nil {
				t.Fatal(err)
			}
			if tt.status == 200 && (result["received"] != true || result["eventId"] != "evt_001") {
				t.Fatalf("unexpected response: %v", result)
			}
			if tt.status == 400 && result["error"] == nil {
				t.Fatal("error is missing")
			}
		})
	}
}

func TestRoutes(t *testing.T) {
	for _, tt := range []struct {
		method, path string
		status       int
	}{
		{"GET", "/", 200},
		{"GET", "/missing", 404},
		{"GET", "/webhooks/orders", 405},
	} {
		recorder := httptest.NewRecorder()
		NewHandler().ServeHTTP(recorder, httptest.NewRequest(tt.method, tt.path, nil))
		if recorder.Code != tt.status {
			t.Errorf("%s %s: got %d, want %d", tt.method, tt.path, recorder.Code, tt.status)
		}
	}
}
