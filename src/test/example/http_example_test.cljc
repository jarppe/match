(ns example.http-example-test
  (:require [clojure.string :as str]
            [clojure.test :as test :refer [deftest testing]]
            #?@(:clj [[clj-http.client :as http]]
                :cljs [["https" :refer [request]]
                       [goog.object :as go]])
            [match.async :refer [matches-async?]]))


#?(:clj
   (defn get-user-todos [user-id]
     (let [p (promise)]
       (future
         (http/get (str "https://jsonplaceholder.typicode.com/users/" user-id "/todos")
                   {:accept :json
                    :as     :json
                    :async  true}
                   (fn [response]
                     (->> (update response :headers (fn [headers]
                                                      (->> headers
                                                           (map (fn [[k v]] [(str/lower-case k) v]))
                                                           (into {}))))
                          (deliver p)))
                   (fn [error]
                     (deliver p error))))
       p))
   :cljs
   (defn get-user-todos [user-id]
     (js/Promise. (fn [resolve reject]
                    (let [body (atom "")]
                      (-> (request (str "https://jsonplaceholder.typicode.com/users/" user-id "/todos")
                                   (fn [^js resp]
                                     (.on resp "data" (fn [chunk]
                                                        (swap! body str chunk)))
                                     (.on resp "end" (fn []
                                                       (resolve {:status  (go/get resp "statusCode")
                                                                 :headers (-> resp
                                                                              (go/get "headers")
                                                                              (js->clj))
                                                                 :body    (-> @body
                                                                              (js/JSON.parse)
                                                                              (js->clj {:keywordize-keys true}))})))))
                          (.on "error" reject)
                          (.end)))))))



(deftest fetch-user-todos
  (testing "Ensure that all TODO's belong to user 1, this time async"
    (matches-async? {:status  200
                     :headers {"content-type" #(str/starts-with? % "application/json")}
                     :body    (fn [body]
                                (every? (comp (partial = 1) :userId) body))}
                    (get-user-todos 1))))
