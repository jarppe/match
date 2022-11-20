(ns node
  (:require [goog.object]
            [match.core]
            [match.async]))


(defn hello []
  "Hello, world!")


(comment
  (js/console.log (hello))
  ;
  )