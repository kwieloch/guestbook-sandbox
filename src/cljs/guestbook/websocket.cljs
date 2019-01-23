(ns guestbook.websocket
  (:require [taoensso.sente :as sente]
            [taoensso.timbre :as log]))

(def ?csrf-token
  (when-let [el (.getElementById js/document "token")]
    (.-value el)))

(if ?csrf-token
  (log/info "CSRF token detected in HTML, great!")
  (log/info "CSRF token NOT detected in HTML, default Sente config will reject requests"))


(let [
      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! 
       "/ws" 
       ?csrf-token 
       {:type :auto})]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(defn handshake-handler [{:as ev-msg :keys [?data]}]
  (log/info "Connection established: " ?data))

(defn state-handler [{:as ev-msg :keys [?data]}]
  (log/info (str "State changed: " ?data)))

(defn default-event-handler [{:as ev-msg :keys [event]}]
  (log/info (str "Unhandled event: " event)))

(defn register-handlers [& [{:keys [message state handshake] 
                             :or {state state-handler handshake handshake-handler}}]]
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
          (sente/start-chsk-router! 
           ch-chsk 
           (register-handlers {:message message-handler
                               :state state-handler
                               :handshake handshake-handler}))))

