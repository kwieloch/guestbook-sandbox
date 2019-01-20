(ns guestbook.websocket
  (:require [taoensso.sente :as sente]))

(def ?csrf-token
  (when-let [el (.getElementById js/document "token")]
    (.-value el)))

(if ?csrf-token
  (.log js/console "CSRF token detected in HTML, great!")
  (.log js/console "CSRF token NOT detected in HTML, default Sente config will reject requests"))


(let [
      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client! 
       "/ws" 
       ?csrf-token 
       {:type :auto})]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(defn handshake-handler [{:keys [?data]}]
  (.log js/console "Connection established: " ?data))

(defn state-handler [{:keys [?data]}]
  (.log js/console (str "State changed: " ?data)))

(defn default-event-handler [ev]
  (.log js/console (str "Unhandled event: " (:event ev))))

(defn register-handlers [& [{:keys [message state handshake] :or {state state-handler handshake handshake-handler}}]]
  (fn [ev-msg]
    (case (:id ev-msg)
      :chsk/handshake (handshake ev-msg)
      :chsk/state (state ev-msg)
      :chsk/recv (message ev-msg)
      (default-event-handler ev-msg))))

(defonce router (atom nil))
(defn stop-router [] (when-let [stop-fn @router] (stop-fn)))
(defn start-router [message-handler]
  (stop-router)
  (reset! router 
          (sente/start-client-chsk-router! ch-chsk (register-handlers {:message message-handler
                                                                       :state state-handler
                                                                       :handshake handshake-handler}))))

