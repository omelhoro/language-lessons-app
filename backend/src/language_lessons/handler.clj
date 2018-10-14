(ns language-lessons.handler
  (:require [compojure.core :refer [GET POST PUT defroutes]]
            [compojure.route :refer [not-found resources]]

            [language-lessons.middleware :refer [wrap-middleware]]

            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.util.response :refer [response file-response resource-response content-type]]

            [datomic.client.api :as d]

            [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.edn :as edn]

            [com.walmartlabs.lacinia :refer [execute]]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :as resolve]

            [ring.middleware.cors :refer [wrap-cors]]

            [clojure.java.io :as jio]

            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.resource :refer [wrap-resource]]

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
  (->
      (jio/resource "schema.edn")
      slurp
      edn/read-string
      (attach-resolvers {:get-people get-people
                         :create-person create-person
                         :update-person update-person
                         :delete-person delete-person})
      schema/compile))


(defroutes routes
  (GET "/*" [] (content-type (resource-response "index.html" {:root "public"})  "text/html"))

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
                                        ]
                                    (json/write-str result))}))
  (not-found "Not Found"))


(defn create-app-state
  []
  (create-db-conn!))

(def app (->
          #'routes
          (wrap-cors :access-control-allow-origin [#"http://localhost:9500"]
                      :access-control-allow-methods [:post])
          (wrap-resource "public")
          wrap-keyword-params
          wrap-json-response
          wrap-json-body
          wrap-middleware
          wrap-gzip))
