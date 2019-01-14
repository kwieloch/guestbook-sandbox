(ns guestbook.websocket
  (:require [cognitect.transit :as t]))

(defonce wsch (atom nil))

(def json-reader (t/reader :json))
(def json-writer (t/writer :json))

(defn wrap-with-decoder [handler]
  (fn [msg]
    (->> msg
         (.-data)
         (t/read json-reader)
         (handler))))

(defn send-message [msg]
  (if @wsch
    (->> msg
         (t/write json-writer)
         (.send @wsch))
    (throw (js/Error. "Websocket is not available"))))

(defn connect! [url receive-handler]
  (if-let [ch (js/WebSocket. url)]
    (do 
      (set! (.-onmessage ch) (wrap-with-decoder receive-handler))
      (reset! wsch ch))
    (throw (js/Error. "WebSocket connection failed!"))))
  
