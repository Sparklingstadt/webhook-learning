package webhook

import (
	"encoding/json"
	"io"
	"log"
	"net/http"
	"strings"
)

// NewHandlerはHonoのappに相当する、URLと処理の組み合わせを作ります。
func NewHandler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /{$}", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, http.StatusOK, map[string]any{
			"message":  "Webhook学習サーバー",
			"endpoint": "POST /webhooks/orders",
		})
	})
	mux.HandleFunc("POST /webhooks/orders", receiveOrder)
	return mux
}

func receiveOrder(w http.ResponseWriter, r *http.Request) {
	// c.req.json()に相当。Unmarshalは余分なJSONが続く本文も拒否します。
	body, err := io.ReadAll(r.Body)
	if err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "本文を読み取れませんでした"})
		return
	}
	var value any
	if err := json.Unmarshal(body, &value); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "本文を正しいJSONで送ってください"})
		return
	}

	// 外部の値は実行時にも確認します。型アサーションの失敗時はゼロ値になります。
	event, _ := value.(map[string]any)
	id, _ := event["id"].(string)
	eventType, _ := event["type"].(string)
	data, _ := event["data"].(map[string]any)
	orderID, _ := data["orderId"].(string)
	if strings.TrimSpace(id) == "" || eventType != "order.created" || strings.TrimSpace(orderID) == "" {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "id、type: order.created、data.orderIdを指定してください"})
		return
	}

	// Hono版と同じく、学習の第1段階ではログ表示だけを行います。
	log.Printf("[受信] event=%s 注文=%s", id, orderID)
	writeJSON(w, http.StatusOK, map[string]any{"received": true, "eventId": id})
}

// c.json()に相当。ヘッダーはステータスや本文より先に設定します。
func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(value); err != nil {
		log.Printf("応答の書き込みに失敗: %v", err)
	}
}
