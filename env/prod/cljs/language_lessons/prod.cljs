(ns language-lessons.prod
  (:require [language-lessons.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
