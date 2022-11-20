(ns match.impl.exeq-clj-test
  (:require [clojure.test :as test :refer [deftest testing is]]
            [match.impl.exeq :as exeq]))

; expected-value expected-form actual path

(deftest accept?-test
  (is (= 1 1)))
