(ns language-lessons.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [reagent.core :as reagent :refer [atom]]
            [language-lessons.core :as rc]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]])
  )

(def isClient (not (nil? (try (.-document js/window)
                              (catch js/Object e nil)))))

(def rflush reagent/flush)

(defn add-test-div [name]
  (let [doc js/document
        body (.-body js/document)
        div (.createElement doc "div")]
    (.appendChild body div)
    div))

(defn with-mounted-component [comp f]
  (when isClient
    (let [div (add-test-div "_testreagent")]
      (let [comp (reagent/render-component comp div #(f comp div))]
        (reagent/unmount-component-at-node div)
        (reagent/flush)
        (.removeChild (.-body js/document) div)))))

(defn found-in [re div]
  (let [res (.-innerHTML div)]
    (if (re-find re res)
      true
      (do (println "Not founda s: " res)
          false))))


;; (deftest test-ajax
;;   (cljs.test/async done
;;     (go
;;       (with-redefs
;;         [ajax.core/GET
;;          (fn
;;            [url {handler :handler}]
;;            (do
;;              (is (= "/api/people" url))
;;              (handler {:body "Hello Igor"})))]
;;         (is (= {:body "Hello Igor"} (<! (rc/get-people-srvc))))
;;         (done)
;;         ))))

(deftest test-home
  (with-mounted-component
    (rc/home-page)
    (fn [c div]
      (is (found-in #"Welcome to" div)))))
