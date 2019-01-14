(ns guestbook.routes.home
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

(defn validate-message [params]
  (first (st/validate params message-schema)))

(defn home-page []
  (layout/render
    "home.html"))

(defn about-page []
  (layout/render "about.html"))

(defn get-messages []
  (response/ok (db/get-messages)))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/messages" [] (get-messages))
  (GET "/about" [] (about-page)))

