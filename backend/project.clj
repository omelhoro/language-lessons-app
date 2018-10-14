(defproject language-lessons "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.datomic/datomic-pro "0.9.5697" :exclusions [com.google.guava/guava]]
                 [ring-server "0.5.0"]
                 [com.walmartlabs/lacinia "0.30.0"]
                 [ring "1.7.0"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-cors "0.1.12"]
                 [compojure "1.6.1"]
                 [prismatic/schema "1.1.9"]
                 [com.datomic/client-pro "0.8.23" :exclusions [org.eclipse.jetty/jetty-http org.eclipse.jetty/jetty-util org.eclipse.jetty/jetty-client]]
                 [info.sunng/ring-jetty9-adapter "0.12.0"]
                 [yogthos/config "1.1.1"]
                 [bk/ring-gzip "0.3.0"]
                 [org.clojure/core.async "0.4.474"]
                 [ring/ring-json "0.4.0"]]

  :repositories {"my.datomic.com" {:url      "https://my.datomic.com/repo"
                                   :username :env/DATOMIC_USER
                                   :password :env/DATOMIC_PASS
                                   }}

  :plugins [
            [lein-ring "0.12.4"]
            ]

  :ring {:handler      language-lessons.handler/app
         :init language-lessons.handler/create-app-state
         :open-browser? false
         :reload-paths ["src" "env"]
         :uberwar-name "language-lessons.war"}


  :uberjar-name "language-lessons.jar"
  :main ^:skip-aot language-lessons.server
  :profiles {:dev     {:repl-options {:init-ns          language-lessons.repl
                                      :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                       :dependencies [
                                      [ring/ring-mock "0.3.2"]
                                      [ring/ring-devel "1.7.0"]
                                      [prone "1.6.1"]
                                      [figwheel-sidecar "0.5.16"]
                                      [org.clojure/tools.nrepl "0.2.13"]
                                      [com.cemerick/piggieback "0.2.2"]
                                      [pjstadig/humane-test-output "0.8.3"]
                                      [cljs-ajax "0.7.5"]
                                      ]

                       :source-paths ["env/dev"]
                       :plugins      [
                                      [cider/cider-nrepl "0.15.1"]
                                      [org.clojure/tools.namespace "0.3.0-alpha4"
                                       :exclusions [org.clojure/tools.reader]]
                                      [refactor-nrepl "2.3.1"
                                       :exclusions [org.clojure/clojure]]
                                       [lein-cloverage "1.0.10"]
                                       [lein-ancient "0.6.15"]
                                       [lein-environ "1.1.0"]
                                      ]

                       :injections   [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]

                       :env          {:dev true}}

             :uberjar {
                       :source-paths ["env/prod"]
                       :env          {:production true}
                       :aot          :all
                       :omit-source  true
                       }})
