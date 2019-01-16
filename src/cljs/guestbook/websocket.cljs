(ns guestbook.websocket
  (:require [taoensso.sente :as sente]))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/ws" {:type :auto})]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(def router (atom nil))

(defn handshake-handler [{:keys [?data]}]
  (.log js/console "Connection established: " ?data))

(defn state-handler [{:keys [?data]}]
  (.log js/console (str "State changed: " ?data)))

(defn default-event-handler [ev]
  (.log js/console (str "Unhandled event: " (:event ev))))

(defn event-message-handler [& [{:keys [message state handshake] :or {state state-handler handshake handshake-handler}}]]
  (fn [ev-msg]
    (case (:id ev-msg)
      :chsk/handshake (handshake ev-msg)
      :chsk/state (state ev-msg)
      :chck/recv (message ev-msg)
      (default-event-handler ev-msg))))

(defn state-handler [{:keys [?data]}]
  (.log js/console (str "state changed; " ?data)))

(defn stop-router []
  (when-let [stop-fn @router] (stop-fn)))

(defn start-router [message-handler]
  (stop-router)
  (reset! router (sente/start-chsk-router! ch-chsk (event-message-handler {:message message-handler
                                                                           :state state-handler
                                                                           :handshake handshake-handler}))))

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
  
