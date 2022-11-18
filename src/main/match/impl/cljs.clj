(ns match.impl.cljs
  (:require [cljs.test :as test]
            [match.impl.run]))


(defmethod cljs.test/assert-expr 'matches? [_env msg form]
  `(cljs.test/do-report
    ~(#'match.impl.run/run msg form)))
