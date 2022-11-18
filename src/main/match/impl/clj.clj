(ns match.impl.clj
  (:require [clojure.test :as test]
            [match.impl.run]))


(defmethod clojure.test/assert-expr 'matches? [msg form]
  `(clojure.test/do-report
    ~(#'match.impl.run/run :clj msg form)))
