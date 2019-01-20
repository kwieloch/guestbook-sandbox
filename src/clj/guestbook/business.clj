(ns guestbook.business
  (:require [guestbook.layout :as layout]
            [guestbook.db.core :as db]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [struct.core :as st]))

(def message-schema
  [[:name
    st/required
    st/string]

   [:message
    st/required
    st/string
    {:message "message must contain at least 10 characters"
     :validate #(> (count %) 9)}]])
