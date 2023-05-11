(ns user
  (:require [shadow.cljs.devtools.api :as shadow]
            [kaocha.repl :as kaocha]
            [kaocha.report]
            [clojure.string :as str]))


(defn repl [build-id]
  (shadow/repl build-id))



(defn repl-web [] (repl :web))


(defn repl-api [] (repl :node))


(defn run-unit-tests []
  (kaocha/run :unit {:reporter kaocha.report/documentation}))


(defn run-all-tests []
  (kaocha/run-all {:reporter kaocha.report/dots}))


(defn run-cljs-tests []
  (shadow/compile :test))


(comment

  (coll? #{})
  (require '[match.impl.exeq :as eq])

  (satisfies? eq/ExtendedEquality {})
  (extends? eq/ExtendedEquality {})

  ; CLJ tests
  (run-unit-tests)

  ; CLJS tests
  (shadow/compile :test)

  (shadow/compile :node)

  ;
  )

