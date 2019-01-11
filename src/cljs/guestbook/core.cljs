(ns guestbook.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST]]))

(defn get-messages [messages]
  (GET "/messages"
       {:headers {"Accept" "application/transit+json"}
        :handler #(reset! messages (vec %))}))

(defn send-message [fields messages errors]
  (POST "/message"
        {:params @fields
         :headers {"Accept" "application/transit+json"
                   "x-csrf-token" (-> js/document (.getElementById "token") (.-value))}
         :handler #(do (get-messages messages)
                       (reset! errors nil)
                       (.log js/console (str {:response %})))
         :error-handler #(do (reset! errors (get-in % [:response :errors]))
                             (.error js/console (str {:error %})))}))

(defn message-list [messages]
  [:ul.content
   (for [{:keys [name message timestamp]} @messages]
     ^{:key timestamp}
     [:li 
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p "--- " name]])])

(defn show-errors [errors field-id]
  (when-let [description (field-id @errors)]
    [:div.alert.alert-danger description]))

(defn message-form [messages]
  (let [fields (reagent/atom nil)
        errors (reagent/atom nil)]
    (fn []
      [:div.content [:div.form-group
                     [:p "name: " (:name @fields)]
                     [:p "message: " (:message @fields)]
                     [:p "errors: " @errors]
                     
                     [:p "Name"] 
                     [:input.form-control {:type :text
                                           :name :name
                                           :value (:name @fields)
                                           :on-change #(swap! fields assoc :name (-> % .-target .-value) )}]
                     [show-errors errors :name]
                     
                     [:p "Message"]
                     [:textarea.form-control {:rows 4 :cols 50
                                              :name :message
                                              :value (:message @fields)
                                              :on-change #(swap! fields assoc :message (-> % .-target .-value) )}]
                     [show-errors errors :message]

                     [:input.btn.btn-primary {:type :submit 
                                              :value "Comment"
                                              :on-click #(send-message fields messages errors)}]]])))


(defn home []
  (let [messages (reagent/atom nil)]
    (get-messages messages)
    (fn []
      [:div
       [:div.row
        [:div.span12 [message-list messages]]]
       [:div.row [:div.span12 [message-form messages]]]
       [:h1 "Hello World from Reagent"]])))

(reagent/render
 [home]
 (.getElementById js/document "content"))
