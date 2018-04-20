(ns language-lessons.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [language-lessons.core-test])
            )

(doo-tests 'language-lessons.core-test)
