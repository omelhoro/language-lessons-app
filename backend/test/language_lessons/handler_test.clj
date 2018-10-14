(ns language-lessons.handler-test
  (:require
    [clojure.test :refer :all]
    [language-lessons.handler :as handler]
    [ring.adapter.jetty :refer [run-jetty]]
    [ajax.core :refer [GET POST]]
    [clojure.java.shell :refer [sh]]
    [clojure.core.async :as async :refer [<!! >! <! go take! >!!]]
    [datomic.api :as d-api]
    ))

(def uri "datomic:mem://test")
(d-api/create-database uri)
(def conn (d-api/connect uri))

(with-redefs [datomic.client.api/transact (fn [-conn data] (d-api/transact conn (:tx-data data)))]
  (handler/set-conn-and-write-schema! conn))

(run-jetty handler/app {:port 4001 :join? false})

;; (deftest is-test-async-post
;;   (let [chan (async/chan)]
;;     (with-redefs [datomic.client.api/transact (fn [-conn {data :tx-data}] (d-api/transact conn data))]
;;       (do (POST "http://localhost:4001/api/people"
;;                 {:response-format :json
;;                  :format          :json
;;                  :handler         (fn [resp] (>!! chan resp))
;;                  :error-handler   (fn [resp] (>!! chan resp))
;;                  :params          {"person/name" "Igor"}
;;                  })
;;           (is (= {"result" "success"} (async/<!! chan)))
;;           ))))

;; (deftest is-test-async
;;   (let [chan (async/chan)]
;;     (with-redefs [datomic.client.api/q d-api/q
;;                   datomic.client.api/db d-api/db]
;;       (do (GET "http://localhost:4001/api/people"
;;                {:response-format :json
;;                 :handler         (fn [resp] (>!! chan resp))}
;;                :error-handler (fn [resp] (>!! chan resp))
;;                )
;;           (is (= 1 (count (async/<!! chan))))
;;           ))))


(defn default-chan []
  (async/chan 1))

(deftest basic-channel-test
  (let [c (default-chan)
        f (future (<!! c))]
    (>!! c 42)
    (is (= @f 42))))
