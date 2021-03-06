(ns guestbook.routes.home
  (:require [compojure.core :refer [defroutes GET]]
            [guestbook.db.core :as db]
            [guestbook.layout :as layout]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" [] (home-page)))

