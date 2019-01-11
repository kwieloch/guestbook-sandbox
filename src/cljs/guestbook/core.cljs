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

(defn message-form []
  (let [fields (reagent/atom nil)]
    (fn []
      [:div.content [:div.form-group
                     [:p "name: " (:name @fields)]
                     [:p "message: " (:message @fields)]
                     [:p "Name"] [:input.form-control {:type :text
                                                       :name :name
                                                       :value (:name @fields)
                                                       :on-change #(swap! fields assoc :name (-> % .-target .-value) )}]
                     [:p "Message"] [:textarea.form-control {:rows 4 :cols 50
                                                             :name :message
                                                             :value (:message @fields)
                                                             :on-change #(swap! fields assoc :message (-> % .-target .-value) )}]
                     [:input.btn.btn-primary {:type :submit :value "Comment"}]]])))


(defn home []
  (let [messages (reagent/atom nil)]
    (get-messages messages)
    (fn []
      [:div
       [:div.row
        [:div.span12 [message-list messages]]]
       [:div.row [:div.span12 [message-form]]]
       [:h1 "Hello World from Reagent"]])))

(reagent/render
 [home]
 (.getElementById js/document "content"))
