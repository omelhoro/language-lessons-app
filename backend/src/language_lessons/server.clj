(ns language-lessons.server
  (:require [language-lessons.handler :refer [create-app-state app]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (do
      (println (str "Starting server on port " port))
      (create-app-state)
      (run-jetty app {:port port :join? false})))
  )
