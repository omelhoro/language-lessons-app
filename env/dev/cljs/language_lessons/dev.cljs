(ns ^:figwheel-no-load language-lessons.dev
  (:require
    [language-lessons.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
