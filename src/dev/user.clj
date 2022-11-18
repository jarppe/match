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

  (run-unit-tests)

  (shadow/compile :node)
  (shadow/compile :test)

  :cljs/quit
  (repl :web)
  (repl :functions)

  (shadow/active-builds)
  ;; => #{:functions :web}

  (shadow/repl :web)
  (js/console.log "Hello!")
  :cljs/quit

  (shadow/repl :functions)
  (js/console.log "Hello")
  :cljs/quit

  ;
  )

