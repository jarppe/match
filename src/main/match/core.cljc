(ns match.core
  (:require [clojure.test]
            [match.impl.assert-expr]
            [match.impl.ex-eq]
            [match.impl.ex-eq-impl]))


;; This is just for static analysys, this is never actually called.

(defn matches? [_expected _actual]
  (throw (ex-info "This shoudl never be called!" {})))
