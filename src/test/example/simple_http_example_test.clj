(ns example.simple-http-example-test
  (:require [example.get-users :as users]
            [clojure.string :as str]
            [clojure.test :as test :refer [deftest is testing]]
            [match.core :refer [matches?]]))


(deftest get-user-todos-test
  (testing "Ensure that all TODO's belong to user 1"
    (is (matches? {:status  200
                   :headers {"Content-Type" #(str/starts-with? % "application/json")}
                   :body    (fn [body]
                              (every? (comp (partial = 1) :userId) body))}
                  (users/get-user-todos 1)))))


(deftest traditional-test
  (let [resp (users/get-user-todos 1)]
    (is (= 200 (:status resp)))
    (is (str/starts-with? (get-in resp [:headers "Content-Type"]) "application/json"))
    (is (every? (comp (partial = 1) :userId) (:body resp)))))


(deftest nested-data-test
  (is (matches? {:foo {:bar {:message "hello"}
                       :biz [{:a 1}
                             {:b 2}
                             :...]}}
                {:foo {:bar {:message "hello"}
                       :biz [{:a 1}
                             {:b 2}
                             {:c 4}]}})))



(deftest simple-example-test
  (let [data {:name    "Ada Lovelace"
              :title   "Countess of Lovelace"
              :dob     "10 December 1815"
              :address {:city    "London"
                        :country "UK"}}]
    (is (matches? {:name    string?
                   :dob     #"\d+ \S+ \d{4}"
                   :address {:city #{"Surrey" "Ross-shire" "London"}}}
                  data))))


#_(deftest simple-failing-example-test
    (let [data {:name    :ada-lovelace
                :title   "Countess of Lovelace"
                :address {:city    "Nottinghamshire"
                          :country "UK"}}]
      (is (matches? {:name    string?
                     :dob     #"\d+ \S+ \d{4}"
                     :address {:city #{"Surrey" "Ross-shire" "London"}}}
                    data))))


(deftest expecting-exception-test
  (is (matches? (ex-info "Permissions denied" {:error "Not allowed"})
                (do (println "Trying something nefarious...")
                    (throw (ex-info "Permissions denied" {:error "Not allowed"
                                                          :id    123})))))
  (is (matches? (java.io.IOException. "File is gone")
                (do (println "Opening file...")
                    (throw (java.io.IOException. "File is gone"))))))
