(ns guestbook.routes.websockets
  (:require [compojure.core :refer [GET POST defroutes wrap-routes]]
            [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit]
            [guestbook.comments :as comments]
            [guestbook.db.core :as db]
            [taoensso.sente :as sente]
            [mount.core :refer [defstate]]
            [taoensso.sente.server-adapters.immutant :refer (get-sch-adapter)]))

(let [{:keys [ajax-post-fn
              ajax-get-or-ws-handshake-fn
              ch-recv
              send-fn 
              connected-uids 
              ]}
      (sente/make-channel-socket! (get-sch-adapter) {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv)
  (def chsk-send!                    send-fn)
  (def connected-uids                connected-uids)
  )

(defn save-message [msg]
  (if-let [errors (comments/validate-message msg)]
    {:errors errors}
    (let [msg-ts (assoc msg :timestamp (java.util.Date.))]
      (db/save-message! msg-ts)
      msg-ts)))

(defn handle-add-message [evt-id msg]
  (let [result (save-message msg)]
    (if (:errors result)
      [:guestbook/message-invalid result]
      [:guestbook/message-added result])))

(defn handle-get-all-messages [evt-id msg]
  [:guestbook/all-messages-retrived (db/get-messages)])

(defn notify-clients [[event-id msg]]
  (doseq [uid (:any @connected-uids)]
    (chsk-send! uid [event-id msg])))

(defn handle-message [{:keys [id ?data ?reply-fn]}]
  (condp = id
    :guestbook/add-message
    (let [[res-evt-id result] (handle-add-message id ?data)]
      (if (= res-evt-id :guestbook/message-added)
        (do (?reply-fn [res-evt-id result])
            (notify-clients [res-evt-id result]))
        (?reply-fn [res-evt-id result])))

    :guestbook/get-all-messages
    (?reply-fn (handle-get-all-messages id ?data))

    (log/debug "Unhandled client event: " id ?data)))

(defn stop-router [stop-fn]
  (when stop-fn (stop-fn)))

(defn start-router []
  (sente/start-chsk-router! ch-chsk handle-message))

(defstate router
  :start (start-router)
  :stop (stop-router router))

(defroutes websocket-routes
  (GET "/ws" request (ring-ajax-get-or-ws-handshake request))
  (POST "/ws" request (ring-ajax-post request)))
