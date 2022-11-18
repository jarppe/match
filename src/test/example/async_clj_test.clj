(ns example.async-clj-test
  (:require [clojure.test :as test :refer [deftest is]]
            [match.core :refer [matches?]]
            [match.async :refer [matches-async?]]))


(deftest sync-test
  (is (matches? {:status (fn [s] (<= 200 s 299))
                 :body   {:user {:email "foo@example.com"}
                          :role #{:admin :super-user}}}
                ; Actual:
                {:status  200
                 :headers {"content-type" "application/edn"}
                 :body    {:user {:id    123
                                  :email "foo@example.com"}
                           :role :admin}})))


(defn http-get []
  (future
    (Thread/sleep 1000)
    {:status  200
     :headers {"content-type" "application/edn"}
     :body    {:user {:id    123
                      :email "foo@example.com"}
               :role :admin}}))


(deftest async-test
  (matches-async? {:status (fn [s] (<= 200 s 299))
                   :body   {:user {:email "foo@example.com"}
                            :role #{:admin :super-user}}}
                  (http-get)))
