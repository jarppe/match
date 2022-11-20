(ns match.impl.assert-expr-cljs
  (:require [cljs.test :as test]
            [match.impl.run]))


(defmethod cljs.test/assert-expr 'matches? [_env msg form]
  `(cljs.test/do-report
    ~(#'match.impl.run/run :cljs msg form)))
