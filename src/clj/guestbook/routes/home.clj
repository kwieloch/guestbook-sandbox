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

(defn save-message! [{:keys [params]}]
  (if-let [errors (validate-message params)]
    (-> (response/found "/")
        (assoc :flash (assoc params :errors errors)))
    (do
      (db/save-message! (assoc params 
                               :timestamp (java.util.Date.)))
      (response/found "/"))))

(defn home-page []
  (layout/render
    "home.html"
    {:messages (db/get-messages)}))

(defn about-page []
  (layout/render "about.html"))

(defn get-messages []
  (response/ok (db/get-messages)))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/messages" [] (get-messages))
  (POST "/message" request (save-message! request))  
  (GET "/about" [] (about-page)))

