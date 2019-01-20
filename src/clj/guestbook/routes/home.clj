(ns guestbook.routes.home
  (:require [compojure.core :refer [defroutes GET]]
            [guestbook.db.core :as db]
            [guestbook.layout :as layout]
            [ring.util.http-response :as response]))

(defn home-page []
  (layout/render "home.html"))

(defn about-page []
  (layout/render "about.html"))

(defn get-messages []
  (response/ok (db/get-messages)))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/messages" [] (get-messages))
  (GET "/about" [] (about-page)))

