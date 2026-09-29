package main

import (
	"log"
	"net/http"
	"os"
	"time"

	"example.com/go-webhook-learning/internal/webhook"
)

func main() {
	// Hono版と同じポートです。同時に起動するときは片方のポートを変えてください。
	host := os.Getenv("HOST")
	if host == "" {
		host = "127.0.0.1"
	}
	server := &http.Server{
		Addr:              host + ":3000",
		Handler:           webhook.NewHandler(),
		ReadHeaderTimeout: 5 * time.Second,
	}
	log.Printf("待受アドレス: %s", server.Addr)
	log.Println("このPC: http://127.0.0.1:3000")
	if host == "0.0.0.0" {
		log.Println("別端末: http://<このPCのLAN IP>:3000")
	}
	log.Fatal(server.ListenAndServe())
}
