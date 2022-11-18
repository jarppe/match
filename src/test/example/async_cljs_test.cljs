(ns example.async-cljs-test
  (:require [clojure.test :as test :refer [deftest is]]
            [match.core :refer [matches?]]))


(defn http-get []
  {:status  200
   :headers {"content-type" "application/edn"}
   :body    {:user {:id    123
                    :email "foo@example.com"}
             :role :admin}})


(deftest fancy
  (is (matches? {:status (fn [s] (<= 200 s 299))
                 :body   {:user {:email "foo@example.com"}
                          :role #{:admin :super-user}}}
                (http-get))))


#_(deftest async-fancy
    (matches-async? {:foo 42}
                    (js/Promise.resolve {:foo 421})))
