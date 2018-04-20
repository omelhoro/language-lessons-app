(ns language-lessons.core
  (:require [reagent.core :as reagent :refer [atom]]
            [secretary.core :as secretary :include-macros true]
            [cljs.core.async :as async]
            [ajax.core :refer [GET POST PUT]]
            [clojure.walk :refer [stringify-keys]]
            [accountant.core :as accountant])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]])
  )

;; -------------------------
;; Views

(defonce state (atom {:name "", :create-new-person? true, :current-person nil, :people []}))

(defn to-string-keys
  [the-map]
  (into (empty the-map) (for [[k v] the-map] [(if-let [nspace (namespace
                                                                k)] (str nspace "/" (name k)) (name k)) v])))

(defn get-people-srvc
  ([chan]
   (GET "/api/people"
        {:response-format :json
         :handler         (fn [resp] (async/put! chan resp))
         })
   chan)
  ([] (get-people-srvc (async/chan))))

(defn create-person-srvc
  []
  (let [ch (async/chan)]
    (POST "/api/people"
          {:response-format :json
           :format          :json
           :handler         (fn [resp] (async/put! ch resp))
           :params          (to-string-keys {:person/name (:name @state)})})
    ch))

(defn update-person-srvc
  [person]
  (let [ch (async/chan)]
    (PUT (str "/api/person/" (get person "db/id"))
         {:response-format :json
          :format          :json
          :handler         (fn [resp] (async/put! ch resp))
          :params          person})
    ch))


(defn create-person
  [evt]
  (do (.preventDefault evt)
      (go
        (let [resp (<! (create-person-srvc))
              people (<! (get-people-srvc))]
          (swap! state assoc :people people)
          ))
      ))

(defn update-person
  [evt]
  (do (.preventDefault evt)
      (go
        (let [resp (<! (update-person-srvc (:current-person @state)))
              people (<! (get-people-srvc))]
          (swap! state assoc :people people)
          (swap! state assoc :current-person nil)
          ))
      ))


(defn home-page []
  [:div [:h2 "Welcome to language-lessons"]
   [:button.ui.primary.button
    {:on-click #(swap! state assoc :current-person nil)}
    "Create new person"]
   [:div.ui.hidden.divider]
   [:form.ui.form
    [:div
     (if (:current-person @state)
       (let [person (:current-person @state)]
         [:div.ui.action.input
          [:input {:value     (get person "person/name")
                   :on-change (fn [evt]
                                (swap!
                                  state
                                  assoc-in
                                  [:current-person "person/name"] (-> evt .-target .-value)))}]
          [:button.ui.button {:on-click update-person} "Update Person"]
          ]
         )
       [:div.ui.action.input
        [:input {:value (:name @state) :on-change (fn [evt] (swap! state assoc :name (-> evt .-target .-value)))}]
        [:button.ui.button {:on-click create-person} "Save Person"]
        ])
     ]
    ]
   [:ul.ui.list
    (map (fn [item]
           [:li.item {:key (get item "db/id")}
            [:span (get item "person/name")]
            [:a {:on-click (fn [] (swap! state assoc :current-person item))} " Edit"]])
         (:people @state))
    ]
   [:h3 (str "# of People = " (count (:people @state)))
    ]
   [:div [:a {:href "/about"} "go to about paged"]]])

(defn about-page []
  [:div [:h2 "About language-lessons"]
   [:div [:a {:href "/"} "go to the home page"]]])

;; -------------------------
;; Routes

(defonce page (atom #'home-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute
  "/"
  []
  (reset! page #'home-page))

(secretary/defroute
  "/about"
  []
  (reset! page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root)
  (go (let [people (<! (get-people-srvc))] (swap! state assoc :people people))))
