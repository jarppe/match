(ns match.impl.accepts
  (:require [match.impl.ex-eq :as exeq]
            [match.impl.util :as u]))


(defn accept-equals [expected-value expected-form actual path]
  (let [r (= expected-value actual)]
    [{:path path
      :type (if r :pass :fail)
      :message (u/sprintf "(= %s %s) => %s"
                          (pr-str expected-form)
                          (pr-str actual)
                          (pr-str r))
      :expected expected-form
      :actual actual}]))


(defn accept-associative [expected-value expected-form actual path]
  (if (not (associative? actual))
    [{:path path
      :type :fail
      :message (u/sprintf "(associative? %s) => false" (pr-str actual))
      :expected expected-form
      :actual actual}]
    (->> (keys expected-value)
         (reduce (fn [acc k]
                   (let [path' (conj path k)]
                     (into acc (if (contains? actual k)
                                 (exeq/accept? (get expected-value k)
                                               (get expected-form k)
                                               (get actual k)
                                               path')
                                 [{:path path'
                                   :type :fail
                                   :message (u/sprintf "actual is missing key %s" (pr-str k))
                                   :expected (get expected-form k)
                                   :actual nil}]))))
                 []))))


(defn accept-sequential [expected-value expected-form actual path]
  (if-not (sequential? actual)
    [{:path path
      :type :fail
      :message (u/sprintf "(sequential? %s) => false" (pr-str actual))
      :expected expected-form
      :actual actual}]
    (->> (map vector
              (concat expected-value [::eof])
              (concat expected-form [::eof])
              (concat actual [::eof])
              (map (partial conj path) (range)))
         (reduce (fn [acc [expected-value expected-form actual path]]
                   (cond
                       ; expected is ..., that means we're done:
                     (= expected-value :...)
                     (reduced acc)

                       ; both ended at the same time:
                     (= expected-value actual ::eof)
                     (reduced acc)

                       ; expected shorter than actual:
                     (and (= expected-value ::eof)
                          (not= actual ::eof))
                     (reduced (conj acc {:path path
                                         :type :fail
                                         :message (u/sprintf "did not expect more than %d elements" (last path))
                                         :expected nil
                                         :actual actual}))

                       ; expected longer than actual:
                     (and (not= expected-value ::eof)
                          (= actual ::eof))
                     (reduced (conj acc {:path path
                                         :type :fail
                                         :message (u/sprintf "expected more than %d elements" (last path))
                                         :expected expected-form
                                         :actual nil}))

                       ; regular deep compare:
                     :else
                     (into acc (exeq/accept? expected-value expected-form actual path))))
                 []))))


(defn accept-set [expected-value expected-form actual path]
  (let [result (contains? expected-value actual)]
    [{:path path
      :type (if result :pass :fail)
      :message (u/sprintf "(contains? %s %s) => %s"
                          (pr-str expected-form)
                          (pr-str actual)
                          (pr-str result))
      :expected expected-form
      :actual actual}]))

(defn accept-fn [expected-value expected-form actual path]
  (let [[status response] (try
                            [:response (expected-value actual)]
                            (catch #?(:clj Throwable :cljs js/Object) e
                              [:ex e]))
        result (and (= status :response) response)]
    (if (exeq/multi-result-response? response)
      (->> response
           :results
           (mapv #(update % :path (partial into path))))
      [{:path path
        :type (if result :pass :fail)
        :message (u/sprintf "(%s %s) => %s"
                            (pr-str expected-form)
                            (pr-str actual)
                            (if (= status :response)
                              (pr-str response)
                              (u/sprintf "exception: %s (message=%s, ex-data=%s)"
                                         (u/exception-class-name response)
                                         (u/exception-message response)
                                         (if-let [data (ex-data response)]
                                           (pr-str data)
                                           "nil"))))
        :expected expected-form
        :actual actual}])))

(defn accept-re [expected-value expected-form actual path]
  (let [result (and (string? actual)
                    (re-find expected-value actual))]
    [{:path path
      :type (if result :pass :fail)
      :message (u/sprintf "(re-find %s %s) => %s"
                          (pr-str expected-form)
                          (pr-str actual)
                          (pr-str result))
      :expected expected-form
      :actual actual}]))

#?(:clj
   (defn accept-class [expected-value expected-form actual path]
     (let [result (instance? expected-value actual)]
       [{:path path
         :type (if result :pass :fail)
         :message (when-not result
                    (u/sprintf "expected instance of %s, but got %s"
                               (some-> expected-value .getName)
                               (some-> actual class .getName)))
         :expected expected-form
         :actual actual}])))

(defn accept-ex-info [expected-value expected-form actual path]
  (if (instance? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo) actual)
    (concat
     (exeq/accept? (ex-message expected-value)
                   (ex-message expected-value)
                   (ex-message actual)
                   (conj path :message))
     (exeq/accept? (ex-data expected-value)
                   (ex-data expected-value)
                   (ex-data actual)
                   (conj path :data)))
    [{:path path
      :type :fail
      :message (u/sprintf "expected instance of %s, but got %s"
                          #?(:clj "clojure.lang.ExceptionInfo" :cljs "cljs.core/ExceptionInfo")
                          (u/exception-class-name actual))
      :expected expected-form
      :actual actual}]))


(defn accept-throwable [expected-value expected-form actual path]
  (if (instance? expected-value actual)
    (exeq/accept? (u/exception-message expected-value)
                  (u/exception-message expected-value)
                  (u/exception-message actual)
                  (conj path :message))
    [{:path path
      :type :fail
      :message (u/sprintf "expected instance of %s, but got %s"
                          (u/exception-class-name expected-value)
                          (u/exception-class-name actual))
      :expected expected-form
      :actual actual}]))


(defn accept-nil [_expected-value expected-form actual path]
  (let [result (nil? actual)]
    [{:path path
      :type (if result :pass :fail)
      :message (u/sprintf "(nil? %s) => %s" (pr-str actual) (pr-str result))
      :expected expected-form
      :actual actual}]))


#_{:clj-kondo/ignore [:dynamic-var-not-earmuffed]}
(def ^:dynamic default-timeout 5000)


#?(:clj 
   (defn accept-delay [expected-value expected-form delayed path] 
          (let [actual (deref delayed default-timeout ::timeout)] 
            (if (not= actual ::timeout)
              (exeq/accept? expected-value
                            expected-form
                            actual
                            path)
              [{:path     path
                :type     :fail
                :message  "timeout"
                :expected expected-form
                :actual   "timeout"}]))))
