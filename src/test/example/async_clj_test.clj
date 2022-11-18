(ns example.async-clj-test
  (:require [clojure.test :as test :refer [deftest is]]
            [match.core :refer [matches?]]
            [match.async :refer [matches-async?]]))


(deftest sync-clj-test
  (is (matches? {:foo 42}
                {:foo 42
                 :bar 1337})))


(deftest async-clj-test
  (matches-async? {:foo 42}
                  (future
                    (Thread/sleep 1000)
                    {:foo 42})))
