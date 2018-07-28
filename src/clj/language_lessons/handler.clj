(ns language-lessons.handler
  (:require [compojure.core :refer [GET POST PUT defroutes]]
            [compojure.route :refer [not-found resources]]

            [hiccup.page :refer [include-js include-css html5]]
            [language-lessons.middleware :refer [wrap-middleware]]

            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.util.response :refer [response]]

            [datomic.client.api :as d]

            [schema.core :as s]

            [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.edn :as edn]

            [com.walmartlabs.lacinia :refer [execute]]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :as resolve]

            [ring.middleware.defaults :refer :all]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]

            [config.core :refer [env]]))

(defonce conn nil)

(def person-schema [{:db/ident       :person/name
                     :db/valueType   :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/doc         "The name of a person"}
                    {:db/ident       :person/gid
                     :db/valueType   :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/doc         "The id of a person"}])

(def peer-host (System/getenv "PEER_HOST"))
(def access-key (System/getenv "DATABASE_AKEY"))
(def db-pass (System/getenv "DATABASE_PASS"))
(def db-name (System/getenv "DATABASE_NAME"))

(defn set-conn-and-write-schema!
  [-conn]
  (do
    (if conn "" (def conn -conn))
    (d/transact conn {:tx-data person-schema})))

(defn create-db-conn!
  ([config]
   (let [cfg config
         client (d/client cfg)
         -conn (d/connect client {:db-name (:db-name config)})]
     (set-conn-and-write-schema! -conn)))
  ([] (create-db-conn! {:server-type :peer-server
                        :access-key  access-key
                        :secret      db-pass
                        :db-name     db-name
                        :endpoint    peer-host})))

(def mount-target
  [:div#app
   [:h3 "ClojureScript has not been compile!"]
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

(defn make-data-simple
  [ent]
  (into {} (map
            (fn [[k v]] [(-> k (name) (clojure.string/replace #".+/" "") (keyword)) v])
            ent)))

(defn get-fields [context]
  (-> context
      (get-in
       [:com.walmartlabs.lacinia.constants/parsed-query :selections 0 :selections])
      ((fn [coll] (mapv #(if
                          (= :id (get % :field))
                           :db/id
                           (get-in %
                                   [:field-definition :qualified-field-name])) coll)))))

(defn find-by-attrs
  [attrs query]
  (apply conj
         '[:find]
         (list 'pull '?e attrs)
         (apply conj
                '[:where]
                query)))

(defn get-people [context arguments value]
  (let [fields-to-get (get-fields context)
        query-result (d/q
                      (find-by-attrs fields-to-get [['?e :person/name]])
                      (d/db conn))
        a (println query-result)]
    (->>
     query-result
     (map first)
     (map make-data-simple))))

(defn make-data-complex [attr-map entity]
  (into {} (map (fn [[k v]]
                  [(if (= k :id) :db/id (->> k (name) (str entity "/") (keyword)))
                   (if (= k :id) (Long/parseLong v) v)]) attr-map)))

(defn update-person [context {args :data} value]
  (let [fields (make-data-complex args "person")
        fields-to-get (get-fields context)
        id (Long/parseLong (:id args))]
    (do
      (d/transact conn {:tx-data [fields]})
      (-> (d/pull (d/db conn) fields-to-get id) (make-data-simple)))))

(defn create-person [context {args :data} value]
  (let [fields (make-data-complex args "person")
        fields-to-get (get-fields context)
        transaction (d/transact conn {:tx-data [fields]})
        id (get-in transaction [:tx-data 1 :e])
        ]
    (-> (d/pull (d/db conn) fields-to-get id) (make-data-simple))))

(defn delete-person [context args value]
  (let [id (Long/parseLong (:id args))]
    (do
      (println id)
      (d/transact conn {:tx-data  [[:db.fn/retractEntity id]]})
      {:id id})))

(def schema
  (-> "./schema.edn"
      slurp
      edn/read-string
      (attach-resolvers {:get-people get-people
                         :create-person create-person
                         :update-person update-person
                         :delete-person delete-person})
      schema/compile))

(defroutes routes
  (GET "/"
    [] (loading-page))
  (GET "/about"
    [] (loading-page))

  (POST "/graphql" [] (fn [request]
                        {:status  200
                         :headers {"Content-Type" "application/json"}
                         :body    (let [query (get-in request [:body "query"])
                                        vars (into {} (map (fn [[k v]] [(keyword k) (keywordize-keys v)]) (get-in request [:body "variables"])))
                                        result (execute
                                                schema
                                                query
                                                vars
                                                nil)
                                        a (println result)
                                        ]
                                    (json/write-str result))}))
  (POST "/api/people"
    []
    (fn [request]
      (do (s/validate {(s/required-key "person/name") (s/both s/Str (s/pred not-empty))} (:body request))
          (d/transact conn {:tx-data [(merge {:person/gid (str (java.util.UUID/randomUUID))} (keywordize-keys (:body request)))]})
          (response {:result "success"}))))

  (resources "/")
  (not-found "Not Found"))

(defn create-app-state
  []
  (create-db-conn!))

(def app (->
          #'routes
          wrap-keyword-params
          wrap-json-response
          wrap-json-body
          wrap-middleware
          wrap-gzip))
