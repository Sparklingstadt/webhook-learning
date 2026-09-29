package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"time"
)

func main() {
	if err := send(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

func send() error {
	// 外部サービスの代わりに送る練習用の通知です。
	event := map[string]any{
		"id": "evt_001", "type": "order.created",
		"data": map[string]string{"orderId": "order_001"},
	}
	body, err := json.Marshal(event)
	if err != nil {
		return err
	}
	client := &http.Client{Timeout: 5 * time.Second}
	response, err := client.Post("http://127.0.0.1:3000/webhooks/orders", "application/json", bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("接続できません。別のターミナルで go run ./cmd/server を起動してください: %w", err)
	}
	defer response.Body.Close()
	result, err := io.ReadAll(response.Body)
	if err != nil {
		return err
	}
	fmt.Println("HTTPステータス:", response.StatusCode)
	fmt.Printf("受信側からの返事: %s\n", result)
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return fmt.Errorf("送信に失敗しました: HTTP %d", response.StatusCode)
	}
	return nil
}
