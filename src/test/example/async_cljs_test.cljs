(ns example.async-cljs-test
  (:require [clojure.test :as test :refer [deftest is]]
            [match.core :refer [matches?]]))

; FIXME:
; [match.async :refer [matches-async?]]


(deftest sanity-check-test
  (is (= 1 1)))


#_(deftest sync-test
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
  (js/Promise. (fn [resolve]
                 (js/setTimeout (fn []
                                  (resolve {:status  200
                                            :headers {"content-type" "application/edn"}
                                            :body    {:user {:id    123
                                                             :email "foo@example.com"}
                                                      :role :admin}}))
                                1000))))

#_(deftest async-test
    (matches-async? {:status (fn [s] (<= 200 s 299))
                     :body   {:user {:email "foo@example.com"}
                              :role #{:admin :super-user}}}
                    (http-get)))
