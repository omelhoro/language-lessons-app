(defproject language-lessons "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [reagent "0.8.2-SNAPSHOT"]
                 [reagent-utils "0.3.1"]
                 [prismatic/schema "1.1.9"]
                 [yogthos/config "1.1.1"]
                 [cljs-ajax "0.7.5"]
                 [district0x/graphql-query "1.0.5"]
                 [org.clojure/clojurescript "1.10.339"
                  :scope "provided"]
                 [org.clojure/core.async "0.4.474"]
                 [com.bhauman/figwheel-main "0.1.9"]
                 [com.bhauman/rebel-readline-cljs "0.1.4"]
                 [secretary "1.2.3"]
                 [venantius/accountant "0.2.4"
                  :exclusions [org.clojure/tools.reader]]]

  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]
            "build-dev" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]}

  :main "language-lessons.core"
  :plugins [
            [lein-cljsbuild "1.1.7"]
            ]
  :doo {
        :debug    true
        :build    "test"
        :alias    {:default [:chrome-no-sandbox]}
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
  :clean-targets ^{:protect false}
    [:target-path
     [:cljsbuild :builds :app :compiler :output-dir]
     [:cljsbuild :builds :app :compiler :output-to]]

  :test-paths ["test"]
  :resource-paths ["resources" "target/cljsbuild"]
  :cljsbuild {:builds {:min
                       {:source-paths ["src" "env/prod"]
                        :compiler
                                      {:output-to     "target/cljsbuild/public/js/app.js"
                                       :output-dir    "target/cljsbuild/public/js"
                                       :source-map    "target/cljsbuild/public/js/app.js.map"
                                       :optimizations :advanced
                                       :pretty-print  false}}
                       :app
                       {:source-paths ["src" "env/dev"]
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
                       {:source-paths ["src" "test"]
                        :compiler     {:main          "language-lessons.doo-runner"
                                       :asset-path    "/js/out"
                                       :output-to     "target/test.js"
                                       :output-dir    "target/cljstest/public/js/out"
                                       :optimizations :none
                                       :pretty-print  true
                                       }}

                       }
              }


  :figwheel {:http-server-root "public"
             ; :server-port      3449
             ; :nrepl-port       7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"
                                "cider.nrepl/cider-middleware"
                                "refactor-nrepl.middleware/wrap-refactor"
                                ]
             :css-dirs         ["resources/public/css"]
             }



  :profiles {:dev     {:repl-options {:init-ns          language-lessons.repl
                                      :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                       :dependencies [[binaryage/devtools "0.9.10"]
                                      [prone "1.6.1"]
                                      [org.clojure/tools.nrepl "0.2.13"]
                                      [com.cemerick/piggieback "0.2.2"]
                                      [pjstadig/humane-test-output "0.8.3"]]

                       :source-paths ["env/dev"]
                       :plugins      [[lein-figwheel "0.5.16"]
                                      [lein-doo "0.1.10"]
                                      [cider/cider-nrepl "0.15.1"]
                                      [lein-cloverage "1.0.10"]
                                      [lein-ancient "0.6.15"]
                                      [org.clojure/tools.namespace "0.3.0-alpha4"
                                       :exclusions [org.clojure/tools.reader]]
                                      [refactor-nrepl "2.3.1"
                                       :exclusions [org.clojure/clojure]]
                                      ]

                       :injections   [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]

                       :env          {:dev true}}})
