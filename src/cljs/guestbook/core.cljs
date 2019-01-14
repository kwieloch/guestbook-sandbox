(ns guestbook.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET]]
            [guestbook.websocket :as ws]))

(defn get-messages [messages]
  (GET "/messages"
       {:headers {"Accept" "application/transit+json"}
        :handler #(reset! messages (vec %))}))

(defn message-list [messages]
  [:ul.content
   (for [{:keys [name message timestamp]} @messages]
     ^{:key timestamp}
     [:li 
      (if timestamp [:time (.toLocaleString timestamp)] [:p "no time"])
      [:p message]
      [:p "--- " name]])])

(defn show-errors [errors field-id]
  (when-let [description (field-id errors)]
    [:div.alert.alert-danger description]))

(defn message-form [fields errors]
  [:div.content [:div.form-group
                 [:p "name: " (:name @fields)]
                 [:p "message: " (:message @fields)]
                 [:p "errors: " @errors]
                 
                 [:p "Name"] 
                 [:input.form-control {:type :text
                                       :name :name
                                       :value (:name @fields)
                                       :on-change #(swap! fields assoc :name (-> % .-target .-value) )}]
                 [show-errors @errors :name]
                 
                 [:p "Message"]
                 [:textarea.form-control {:rows 4 :cols 50
                                          :name :message
                                          :value (:message @fields)
                                          :on-change #(swap! fields assoc :message (-> % .-target .-value) )}]
                 [show-errors @errors :message]

                 [:input.btn.btn-primary {:type :submit 
                                          :value "Comment"
                                          :on-click #(ws/send-message @fields)}]]])

(defn make-handler [messages fields errors]
  (fn [msg]
    (if-let [result-errors (:errors msg)]
      (reset! errors result-errors)
      (do
        (reset! errors nil)
        (reset! fields nil)
        (swap! messages conj msg)))))

(defn home []
  (let [messages (reagent/atom nil)
        fields (reagent/atom nil)
        errors (reagent/atom nil)]
    (ws/connect! (str "ws://" (.-host js/location) "/ws")
                 (make-handler messages fields errors))
    (get-messages messages)
    (fn []
      [:div
       [:div.row
        [:div.span12 [message-list messages]]]
       [:div.row [:div.span12 [message-form fields errors]]]
       [:h1 "Hello World from Reagent"]])))

(reagent/render
 [home]
 (.getElementById js/document "content"))
