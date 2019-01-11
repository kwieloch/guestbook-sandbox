(ns guestbook.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET]]))

(defn get-messages [messages]
  (GET "/messages"
       {:headers {"Accept" "application/transit+json"}
        :handler #(reset! messages (vec %))}))

(defn message-list [messages]
  [:ul.content
   (for [{:keys [name message timestamp]} @messages]
     ^{:key timestamp}
     [:li 
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p "--- " name]])])

(defn home []
  (let [messages (reagent/atom nil)]
    (get-messages messages)
    (fn []
      [:div
       [:div.row
        [:div.span12 [message-list messages]]]
       [:h1 "Hello World from Reagent"]])))

(reagent/render
 [home]
 (.getElementById js/document "content"))
