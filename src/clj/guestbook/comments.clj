(ns guestbook.comments
  (:require [struct.core :as st]
            [guestbook.db.core :as db]
            [clojure.tools.logging :as log]))

(def message-schema
  [[:name
    st/required
    st/string]
   [:message
    st/required
    st/string
    {:message "message must contain at least 10 characters"
     :validate #(> (count %) 9)}]])

(defn validate-message [msg]
  (first (st/validate msg message-schema)))

(defn handle-add-message [evt-id msg]
  (if-let [errors (validate-message msg)]
    [:guestbook/message-invalid {:errors errors}]
    (let [timed-msg (assoc msg :timestamp (java.util.Date.))]
      (try 
        (db/save-message! timed-msg)
        [:guestbook/message-added timed-msg]
        (catch Exception e 
          (log/error "Error while saving message: " evt-id timed-msg)
          [:guestbook/message-not-saved])))))

(defn handle-get-all-messages [evt-id msg]
  [:guestbook/all-messages-retrived (db/get-messages)])

