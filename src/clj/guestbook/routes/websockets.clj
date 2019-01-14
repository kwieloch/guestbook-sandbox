(ns guestbook.routes.websockets
  (:require [compojure.core :refer [GET defroutes wrap-routes]]
            [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit]
            [guestbook.routes.home :as bl]
            [guestbook.db.core :as db]))

(defonce channels (atom #{}))

(defn connect! [channel]
  (log/info "Channel open")
  (swap! channels conj channel))

(defn disconnect! [channel {:keys [code reason]}]
  (log/info "Channel close, code:  " code "reason:" reason)
  (swap! channels #(remove #{channel} %)))

(defn notify-clients! [msg]
  (doseq [channel @channels]
    (async/send! channel msg)))

(defn save-message [msg]
  (if-let [errors (bl/validate-message msg)]
    {:errors errors}
    (let [msg-ts (assoc msg :timestamp (java.util.Date.))]
      (db/save-message! msg-ts)
      msg-ts)))

(defn encode-transit [message]
  (let [out    (java.io.ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer message)
    (.toString out)))

(defn decode-transit [message]
  (let [in (java.io.ByteArrayInputStream. (.getBytes message))
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn handle-message [channel msg]
  (let [result (save-message (decode-transit msg))]
    (println result)
    (if (:errors result)
      (async/send! channel (encode-transit result))
      #_(notify-clients! (encode-transit result))
      (doseq [c @channels]
        (log/debug result)
        (async/send! c (encode-transit result))))))

(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open connect!
   :on-close disconnect!
   :on-message handle-message})

(defn ws-handler [request]
  (async/as-channel request websocket-callbacks))

(defroutes websocket-routes
  (GET "/ws" request (ws-handler request)))
