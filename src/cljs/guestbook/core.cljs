(ns guestbook.core
  (:require [reagent.core :as reagent :refer [atom]]
            [guestbook.websocket :as ws]
            [taoensso.encore :as encore :refer-macros (have have?)]
            [taoensso.timbre :as log]
            [taoensso.sente :as sente]))

(defn newest-first [msgs]
  (->> msgs (sort-by :timestamp) (reverse)))

(defn reload-messages [messages]
  (fn []
    (ws/chsk-send! [:guestbook/reload-messages "Could you please?"] 
                   8000
                   (fn [[event-type msgs]]
                     (reset! messages (newest-first msgs))))))

(defn feedback-handler [fields errors]
  (fn [[event-id msg]]
    (if-let [result-errors (:errors msg)]
      (reset! errors result-errors)
      (do
        (reset! errors nil)
        (reset! fields nil)))))

(defn message-saved-handler [messages]
  (fn [{[event-type msg] :?data}]
    (when (= event-type :guestbook/message-added)
      (do
        (swap! messages conj msg)
        (log/debug "new message: " msg)))))

(defn message-list [messages]
  [:ul.content
   (for [{:keys [name message timestamp]} @messages]
     ^{:key (or timestamp (.getTime (js/Date.)))}
     [:li 
      (if timestamp 
        [:time (.toLocaleString timestamp)]
        [:p "no time"])
      [:p message]
      [:p "--- " name]])])

(defn show-errors [errors field-id]
  (when-let [description (field-id errors)]
    [:div.alert.alert-danger description]))

(defn message-form [fields errors]
  [:div.content 
   [:div.form-group
    [:p "name: " (:name @fields)]
    [:p "message: " (:message @fields)]
    [:p "errors: " @errors]
                 
    [:p "Name"] 
    [:input.form-control {:type :text
                          :name :name
                          :value (:name @fields)
                          :on-change #(swap! fields assoc :name (-> % .-target .-value))}]
    [show-errors @errors :name]
                 
    [:p "Message"]
    [:textarea.form-control {:rows 4 :cols 50
                             :name :message
                             :value (:message @fields)
                             :on-change #(swap! fields assoc :message (-> % .-target .-value) )}]
    [show-errors @errors :message]

    [:input.btn.btn-primary {:type :submit 
                             :value "Comment"
                             :on-click (fn [ui-ev]
                                         (ws/chsk-send! [:guestbook/add-message @fields] 
                                                        6000
                                                        (feedback-handler fields errors)))}]]])

(defn home []
  (let [messages (reagent/atom nil)
        fields   (reagent/atom nil)
        errors   (reagent/atom nil)]
    (ws/start-router (message-saved-handler messages) 
                     [(reload-messages messages)])
    (fn []
      (reload-messages messages)
      [:div
       [:div.row
        [:div.span12 [message-form fields errors]]]
       [:div.row
        [:div.span12 [message-list messages]]]])))

(reagent/render
 [home]
 (.getElementById js/document "content"))
