(ns node)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn hello []
  "Hello, world!")

(comment
  (js/console.log (hello))
  ;
  )