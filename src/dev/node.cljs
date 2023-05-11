(ns node
  (:require [goog.object]
            [match.core]
            [match.async]))


(defn hello []
  "Hello, world!")


(comment

  (satisfies? ICollection {})
  (coll? {})

  cljs.core/PersistentArrayMap

  (js/console.log (hello))
  ;
  )