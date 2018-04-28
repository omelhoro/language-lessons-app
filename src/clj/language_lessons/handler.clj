(ns language-lessons.handler
  (:require [compojure.core :refer [GET POST PUT defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [language-lessons.middleware :refer [wrap-middleware]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.util.response :refer [response]]
            [datomic.client.api :as d]
    ;[datomic.api :as d]
            [clojure.walk :refer [keywordize-keys]]
            [schema.core :as s
             ;:include-macros true                           ;; cljs only
             ]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [config.core :refer [env]])
  (:gen-class))

(def conn nil)

(def person-schema [{:db/ident       :person/name
                     :db/valueType   :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/doc         "A name of a person"}])

(def peer-host (System/getenv "PEER_HOST"))
(def access-key (System/getenv "DATABASE_AKEY"))
(def db-pass (System/getenv "DATABASE_PASS"))
(def db-name (System/getenv "DATABASE_NAME"))

(defn set-conn-and-write-schema!
  [-conn]
  (do
    (def conn -conn)
    (d/transact conn {:tx-data person-schema})
    ))

(defn create-db-conn!
  ([config]
   (let [cfg config
         client (d/client cfg)
         -conn (d/connect client {:db-name (:db-name config)})
         ]
     (set-conn-and-write-schema! -conn)
     ))
  ([] (create-db-conn! {:server-type :peer-server
                       :access-key  access-key
                       :secret      db-pass
                       :db-name     db-name
                       :endpoint    peer-host})))

(def mount-target
  [:div#app
   [:h3 "ClojureScript has not been compiled!"]
   [:p "please run "
    [:b "lein figwheel"]
    " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name    "viewport"
           :content "width=device-width, initial-scale=1"}]
;   (include-js "https://code.jquery.com/jquery-3.1.1.min.js")
;   (include-js "https://cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.3.1/semantic.js")
   (include-css "https://cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.3.1/semantic.css")
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(defroutes routes
           (GET "/"
                [] (loading-page))
           (GET "/about"
                [] (loading-page))
           (GET "/api/people"
                []
             (fn [request]
               (let [db (d/db conn)
                     results (d/q
                               '[:find (pull ?e [:person/name :db/id])
                                 :where [?e :person/name ?name]] db)]
                 (response (->> results (map #(nth % 0)) (filter #(not-empty (:person/name %)))))
                 )
               ))
           (POST "/api/people"
                 []
             (fn [request]
               (do (s/validate {(s/required-key "person/name") (s/both s/Str (s/pred not-empty))} (:body request))
                   (d/transact conn {:tx-data [(keywordize-keys (:body request))]})
                   (response {:result "success"}))
               ))
           (PUT "/api/person/:id"
                [id]
             (fn [request]
               (do (s/validate {(s/required-key "person/name") (s/both s/Str (s/pred not-empty))
                                (s/required-key "db/id")       s/Int}
                               (:body request))
                   (d/transact conn {:tx-data [(keywordize-keys (:body request))]})
                   (response {:result "success"}))
               ))
           (resources "/")
           (not-found "Not Found")
           )

(defn create-app-state
  []
  (create-db-conn!))

(def app (->
             #'routes
             wrap-keyword-params
             wrap-json-response
             wrap-json-body
             wrap-middleware
             wrap-gzip
             ))
