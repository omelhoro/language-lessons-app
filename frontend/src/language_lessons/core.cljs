(ns ^:figwheel-hooks language-lessons.core
  (:require [reagent.core :as reagent :refer [atom]]
            [secretary.core :as secretary :include-macros true]
            [ajax.core :refer [POST]]
            [clojure.walk :refer [stringify-keys]]
            [graphql-query.core :refer [graphql-query]]
            [accountant.core :as accountant])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

;; -------------------------
;; Views

(defonce state (atom {:new-person {}, :people-to-edit {}}))

(def endpoint (if (= js/location.host "localhost:3449") "http://localhost:3000" ""))

(defn position
  [pred coll]
  (first (keep-indexed
          (fn [idx x]
            (when (pred x)
              idx))
          coll)))

(defn query-service-cb [query vars {success-cb :success error-cb :error}]
  (POST (str endpoint "/graphql")
    {:response-format :json
     :format          :json
     :error-handler   (fn [err] (error-cb err))
     :handler         (fn [resp] (success-cb resp))
     :params          {:query  query
                       :variables vars}}))

(defonce graphql-cache (atom {}))

(defn graphql [{op :op query :query {after-fetch :after-fetch} :hooks} children]
  (let [-state {:loading true :data nil :error nil}
        fetch (fn [{vars :vars}] (query-service-cb query
                                                   vars
                                                   {:success (fn [{data "data"}] (do
                                                                                   (if after-fetch (after-fetch data))
                                                                                   (swap! graphql-cache assoc query (merge -state {:data data :loading false}))))
                                                    :error   #(reset! -state (merge @-state {:error % :loading false}))}))
        first-fetch (if (= op :query) (fetch {}))]
    (fn [] (children (get @graphql-cache query) {:fetch fetch}))))

(def list-people-q (graphql-query {:queries [[:people [:name :id]]]}))

(defn delete-person [id]
  (graphql
   {:op :mutate
    :query (graphql-query {:operation {:operation/type :mutation
                                       :operation/name "delete"}
                           :variables [{:variable/name :$id
                                        :variable/type :ID!}]
                           :queries [[:delete_person {:id :$id} [:id]]]})
    :hooks {:after-fetch (fn [{person "delete_person"}]
                           (swap!
                            graphql-cache
                            update-in
                            [list-people-q :data "people"]
                            #(->> %
                                  (position (fn [item] (= (get item "id") (get person "id"))))
                                  ((fn [ix] (into [] (concat (subvec % 0 ix) (subvec % (inc ix)))))))))}}
   (fn [data {fetch :fetch}]
     [:button.ui.red.button.mini {:on-click #(fetch {:vars {:id id}})} "Delete"])))

(defn edit-person [id state]
  (graphql
   {:op :mutate
    :query (graphql-query {:operation {:operation/type :mutation
                                       :operation/name "update"}
                           :variables [{:variable/name :$data
                                        :variable/type :person_template!}]
                           :queries [[:update_person {:data :$data}
                                      [:name :id]]]})
    :hooks {:after-fetch (fn [{person "update_person"}]
                           (do (swap!
                            graphql-cache
                            update-in
                            [list-people-q :data "people"]
                            #(->> %
                                  (position (fn [item] (= (get item "id") (get person "id"))))
                                  ((fn [ix] (update % ix (fn [val] (merge val person)))))))
                            (swap! state update-in [:people-to-edit] #(dissoc % id))
)
                                  )}}
   (fn [data {fetch :fetch}]
     (let [{name "name" id "id"} (get-in @state [:people-to-edit id])]
       [:div [:form.ui.form.mini [:div.ui.action.input
                                  [:input {:style {:width "130px"}
                                           :value name
                                           :on-change (fn [evt] (->
                                                                 evt
                                                                 .-target
                                                                 .-value
                                                                 ((fn [name] (swap! state assoc-in [:people-to-edit id "name"] name)))))}]
                                  [:button.ui.green.button.mini {:on-click (fn [evt]
                                                                             (do
                                                                               (.preventDefault evt)
                                                                               (fetch {:vars {:data (get-in @state [:people-to-edit id])}})))}
                                   "Update"]]]]))))

(defn list-people []
  (graphql
   {:op :query
    :query  list-people-q}
   (fn [{error :error loading :loading {people "people"} :data} {fetch :fetch}]
     (let []
       (cond
         loading [:div "Loading"]
         error [:div "Error"]
         people [:div
                 [:h3 {:style {:text-align "center"}} (str "Count: " (count people))]
                 [:div.ui.list
                  {:style {:display "table" :margin "0 auto"}}
                  (doall (map (fn [{id "id" name "name" :as person}]
                                (let [is-in-edit (get-in @state [:people-to-edit id])]
                                  [:li.item {:key id :style {:display "flex"}}
                                   [:div
                                   {:style {:width "130px"}}
                                    (if is-in-edit
                                      [edit-person id state]
                                      [:span
                                       name])]
                                   (if (not is-in-edit) [:div [:button.ui.button.purple.mini {:on-click (fn []
                                                                                 (swap! state assoc-in [:people-to-edit id] person))} " Edit"] [delete-person id]]
                                    "")
                                   ]))
                              people))]])))))

(defn create-person [state]
  (graphql
   {:op :mutate
    :query (graphql-query {:operation {:operation/type :mutation
                                       :operation/name "create"}
                           :variables [{:variable/name :$data
                                        :variable/type :person_template!}]
                           :queries [[:create_person {:data :$data}
                                      [:name :id]]]})
    :hooks {:after-fetch (fn [{person "create_person"}]
                           (swap!
                            graphql-cache
                            update-in
                            [list-people-q :data "people"]
                            #(into [person] %)))}}
   (fn [data {fetch :fetch}]
     (let [name (get-in @state [:new-person "name"] "")]
       [:div {:style {:margin "0 auto" :display "table"}} [:form.ui.form [:div.ui.action.input
                             [:input {:value name
                                      :style {:max-width "170px"}
                                      :on-change (fn [evt] (->
                                                            evt
                                                            .-target
                                                            .-value
                                                            ((fn [name] (swap! state assoc-in [:new-person "name"] name)))))}]
                             [:button.ui.button {:on-click (fn [evt]
                                                             (do
                                                               (.preventDefault evt)
                                                               (fetch {:vars {:data (get @state :new-person)}})))}
                              "Create Person"]]]]))))


(defn home-page []
  [:div [:h2 {:style {:text-align "center"}} "Welcome to the Datomic Demo"]
   [create-person state]
   [:div.ui.hidden.divider]
   [list-people]])

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
  (mount-root))

(set! (.-onload js/window) init!)

(defn ^:after-load re-render []
  (mount-root))
