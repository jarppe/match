(ns user
  (:require [shadow.cljs.devtools.api :as shadow]
            [kaocha.repl :as kaocha]
            [kaocha.report]))


(defn repl [build-id]
  (shadow/repl build-id))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn repl-web [] (repl :web))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn repl-api [] (repl :node))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn run-unit-tests []
  (kaocha/run :unit {:reporter kaocha.report/documentation}))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn run-all-tests []
  (kaocha/run-all {:reporter kaocha.report/dots}))


(comment

  ; CLJ tests
  (run-unit-tests)

  ; CLJS tests
  (shadow/compile :test)

  (shadow/compile :node)

  ;
  )

