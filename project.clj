(defproject language-lessons "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.datomic/datomic-pro "0.9.5697" :exclusions [com.google.guava/guava]]
                 [ring-server "0.5.0"]
                 [reagent "0.8.1"]
                 [com.walmartlabs/lacinia "0.28.0"]
                 [reagent-utils "0.3.1"]
                 [ring "1.6.3"]
                 [ring/ring-defaults "0.3.2"]
                 [compojure "1.6.1"]
                 [com.datomic/client-pro "0.8.17" :exclusions [org.eclipse.jetty/jetty-http org.eclipse.jetty/jetty-util org.eclipse.jetty/jetty-client]]
                 [info.sunng/ring-jetty9-adapter "0.11.2"]
                 [hiccup "1.0.5"]
                 [prismatic/schema "1.1.9"]
                 [yogthos/config "1.1.1"]
                 [cljs-ajax "0.7.4"]
                 [bk/ring-gzip "0.3.0"]
                 [district0x/graphql-query "1.0.5"]
                 [org.clojure/clojurescript "1.10.339"
                  :scope "provided"]
                 [org.clojure/core.async "0.4.474"]
                 [ring/ring-json "0.4.0"]
                 [secretary "1.2.3"]
                 [venantius/accountant "0.2.4"
                  :exclusions [org.clojure/tools.reader]]]

  :repositories {"my.datomic.com" {:url      "https://my.datomic.com/repo"
                                   :username :env/DATOMIC_USER
                                   :password :env/DATOMIC_PASS
                                   }}

  :plugins [[lein-environ "1.1.0"]
            [lein-cljsbuild "1.1.7"]
            [lein-cloverage "1.0.10"]
            [lein-ancient "0.6.15"]
            [lein-asset-minifier "0.2.7"
             :exclusions [org.clojure/clojure]]]

  :ring {:handler      language-lessons.handler/app
         :uberwar-name "language-lessons.war"}

  :doo {
        :debug    false
        :build    "test"
        :alias    {:default [:chrome-headless]}
        :coverage {:packages ["language-lessons.core" language-lessons]
                   :reporter {:type "text"}}
        :karma
                  {:launchers {:chrome-no-sandbox {:plugin "karma-chrome-launcher"
                                                   :name   "Chrome_no_sandbox"}}
                   :config    {"customLaunchers"
                               {"Chrome_no_sandbox" {"base"  "ChromeHeadless"
                                                     "flags" ["--no-sandbox"]}}}
                   }
        }


  :min-lein-version "2.5.0"
  :uberjar-name "language-lessons.jar"
  :main ^:skip-aot language-lessons.server
  :clean-targets ^{:protect false}
[:target-path
 [:cljsbuild :builds :app :compiler :output-dir]
 [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  :test-paths ["test/clj"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:min
                       {:source-paths ["src/cljs" "src/cljc" "env/prod/cljs"]
                        :compiler
                                      {:output-to     "target/cljsbuild/public/js/app.js"
                                       :output-dir    "target/cljsbuild/public/js"
                                       :source-map    "target/cljsbuild/public/js/app.js.map"
                                       :optimizations :advanced
                                       :pretty-print  false}}
                       :app
                       {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                        :figwheel     {:on-jsload "language-lessons.core/mount-root"}
                        :compiler
                                      {:main          "language-lessons.dev"
                                       :asset-path    "/js/out"
                                       :output-to     "target/cljsbuild/public/js/app.js"
                                       :output-dir    "target/cljsbuild/public/js/out"
                                       :source-map    true
                                       :optimizations :none
                                       :pretty-print  true}}
                       :test
                       {:source-paths ["src/cljs" "src/cljc" "test/cljs"]
                        :compiler     {:main          language-lessons.doo-runner
                                       :asset-path    "/js/out"
                                       :output-to     "target/test.js"
                                       :output-dir    "target/cljstest/public/js/out"
                                       :optimizations :none
                                       :pretty-print  true
                                       }}

                       }
              }


  :figwheel {:http-server-root "public"
             :server-port      3449
             :nrepl-port       7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"
                                "cider.nrepl/cider-middleware"
                                "refactor-nrepl.middleware/wrap-refactor"
                                ]
             :css-dirs         ["resources/public/css"]
             :init             language-lessons.handler/create-app-state
             :ring-handler     language-lessons.handler/app
             }



  :profiles {:dev     {:repl-options {:init-ns          language-lessons.repl
                                      :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                       :dependencies [[binaryage/devtools "0.9.10"]
                                      [ring/ring-mock "0.3.2"]
                                      [ring/ring-devel "1.6.3"]
                                      [prone "1.6.0"]
                                      [figwheel-sidecar "0.5.16"]
                                      [org.clojure/tools.nrepl "0.2.13"]
                                      [com.cemerick/piggieback "0.2.2"]
                                      [pjstadig/humane-test-output "0.8.3"]

                                      ]

                       :source-paths ["env/dev/clj"]
                       :plugins      [[lein-figwheel "0.5.16"]
                                      [lein-doo "0.1.10"]
                                      [cider/cider-nrepl "0.15.1"]
                                      [org.clojure/tools.namespace "0.3.0-alpha4"
                                       :exclusions [org.clojure/tools.reader]]
                                      [refactor-nrepl "2.3.1"
                                       :exclusions [org.clojure/clojure]]
                                      ]

                       :injections   [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]

                       :env          {:dev true}}

             :uberjar {
                       ; :hooks        [minify-assets.plugin/hooks]
                       :source-paths ["env/prod/clj"]
                       ; :prep-tasks   ["compile" ["cljsbuild" "once" "min"]]
                       :env          {:production true}
                       :aot          :all
                       :omit-source  true
                       }})
