(ns guestbook.core
  (:require [reagent.core :as reagent]))

(defn home []
  [:h2 "Hello World from Reagent!"])

(reagent/render
 [home]
 (.getElementById js/document "content"))
