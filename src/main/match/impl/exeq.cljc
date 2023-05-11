(ns match.impl.exeq
  (:require [match.impl.util :as u]))

;;
;; MultiResultResponse:
;; 
;; Type functions can use to return test responses. Used to distinguish test 
;; reports from normal responses.
;;


(defrecord MultiResultResponse [results])


(defn multi-result-response? [response]
  (instance? MultiResultResponse response))


(defn multi-result-response [results]
  (->MultiResultResponse results))


;;
;; ExtendedEquality
;;


(defprotocol ExtendedEquality
  "Protocol for extended equality"
  (accept? [expected-value expected-form actual path]
    "compare the `expected-value` to `actual` using the extended equality."))


;;
;; Matching utilities:
;;


(defn accept-equals [expected-value expected-form actual path]
  (let [r (= expected-value actual)]
    [{:path     path
      :type     (if r :pass :fail)
      :message  (u/sprintf "(= %s %s) => %s"
                           (pr-str expected-form)
                           (pr-str actual)
                           (pr-str r))
      :expected expected-form
      :actual   actual
      :matcher  'accept-equals}]))


(defn accept-map [expected-value expected-form actual path]
  (if (not (map? actual))
    [{:path     path
      :type     :fail
      :message  (u/sprintf "(map? %s) => false" (pr-str actual))
      :expected expected-form
      :actual   actual
      :matcher  'accept-associative}]
    (->> (keys expected-value)
         (reduce (fn [acc k]
                   (let [path' (conj path k)]
                     (into acc (if (contains? actual k)
                                 (accept? (get expected-value k)
                                          (get expected-form k)
                                          (get actual k)
                                          path')
                                 [{:path     path'
                                   :type     :fail
                                   :message  (u/sprintf "actual is missing key %s" (pr-str k))
                                   :expected (get expected-form k)
                                   :actual   nil
                                   :matcher  'accept-associative}]))))
                 []))))


(defn accept-sequential [expected-value expected-form actual path]
  (if-not (sequential? actual)
    [{:path     path
      :type     :fail
      :message  (u/sprintf "(sequential? %s) => false" (pr-str actual))
      :expected expected-form
      :actual   actual
      :matcher  'accept-sequential}]
    (->> (map vector
              (concat expected-value [::eof])
              (concat expected-form [::eof])
              (concat actual [::eof])
              (map (partial conj path) (range)))
         (reduce (fn [acc [expected-value expected-form actual path]]
                   (cond
                       ; expected is ..., that means we're done:
                     (#{:... '...} expected-value)
                     (reduced acc)

                       ; both ended at the same time:
                     (= expected-value actual ::eof)
                     (reduced acc)

                       ; expected shorter than actual:
                     (and (= expected-value ::eof)
                          (not= actual ::eof))
                     (reduced (conj acc {:path     path
                                         :type     :fail
                                         :message  (u/sprintf "did not expect more than %d elements" (last path))
                                         :expected nil
                                         :actual   actual
                                         :matcher  'accept-sequential}))

                       ; expected longer than actual:
                     (and (not= expected-value ::eof)
                          (= actual ::eof))
                     (reduced (conj acc {:path path
                                         :type :fail
                                         :message (u/sprintf "expected more than %d elements" (last path))
                                         :expected expected-form
                                         :actual nil
                                         :matcher  'accept-sequential}))

                       ; regular deep compare:
                     :else
                     (into acc (accept? expected-value expected-form actual path))))
                 []))))


(defn accept-set [expected-value expected-form actual path]
  (let [result (contains? expected-value actual)]
    [{:path     path
      :type     (if result :pass :fail)
      :message  (u/sprintf "(contains? %s %s) => %s"
                           (pr-str expected-form)
                           (pr-str actual)
                           (pr-str result))
      :expected expected-form
      :actual   actual
      :matcher  'accept-set}]))



(defn accept-collection [expected-value expected-form actual path]
  (cond
    (map? expected-value) (accept-map expected-value expected-form actual path)
    (set? expected-value) (accept-set expected-value expected-form actual path)
    (sequential? expected-value) (accept-sequential expected-value expected-form actual path)
    :else (throw (ex-info (str "unsupported collection: " (type expected-value))
                          {:error "unsupported collection"
                           :value expected-value
                           :type  (type expected-value)}))))


(defn accept-fn [expected-value expected-form actual path]
  (let [[status response] (try
                            [:response (expected-value actual)]
                            (catch #?(:clj Throwable :cljs js/Object) e
                              [:ex e]))
        result (and (= status :response) response)]
    (if (multi-result-response? response)
      (->> response
           :results
           (mapv #(update % :path (partial into path))))
      [{:path     path
        :type     (if result :pass :fail)
        :message  (u/sprintf "(%s %s) => %s"
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
        :actual   actual
        :matcher  'accept-fn}])))


(defn accept-re [expected-value expected-form actual path]
  (let [result (and (string? actual)
                    (re-find expected-value actual))]
    [{:path     path
      :type     (if result :pass :fail)
      :message  (u/sprintf "(re-find %s %s) => %s"
                           (pr-str expected-form)
                           (pr-str actual)
                           (pr-str result))
      :expected expected-form
      :actual   actual
      :matcher  'accept-re}]))


#?(:clj
   (defn accept-class [expected-value expected-form actual path]
     (let [result (instance? expected-value actual)]
       [{:path     path
         :type     (if result :pass :fail)
         :message  (when-not result
                     (u/sprintf "expected instance of %s, but got %s"
                                (some-> expected-value .getName)
                                (some-> actual class .getName)))
         :expected expected-form
         :actual   actual
         :matcher  'accept-class}])))


(defn accept-ex-info [expected-value expected-form actual path]
  (if (instance? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo) actual)
    (concat
     (accept? (ex-message expected-value)
              (ex-message expected-value)
              (ex-message actual)
              (conj path :message))
     (accept? (ex-data expected-value)
              (ex-data expected-value)
              (ex-data actual)
              (conj path :data)))
    [{:path     path
      :type     :fail
      :message  (u/sprintf "expected instance of %s, but got %s"
                           #?(:clj "clojure.lang.ExceptionInfo" :cljs "cljs.core/ExceptionInfo")
                           (u/exception-class-name actual))
      :expected expected-form
      :actual   actual
      :matcher  'accept-ex-info}]))


(defn accept-throwable [expected-value expected-form actual path]
  (if (instance? (type expected-value) actual)
    (accept? (u/exception-message expected-value)
             (u/exception-message expected-value)
             (u/exception-message actual)
             (conj path :message))
    [{:path     path
      :type     :fail
      :message  (u/sprintf "expected instance of %s, but got %s"
                           (u/exception-class-name expected-value)
                           (u/exception-class-name actual))
      :expected expected-form
      :actual   actual
      :matcher  'accept-throwable}]))


(defn accept-nil [_expected-value expected-form actual path]
  (let [result (nil? actual)]
    [{:path     path
      :type     (if result :pass :fail)
      :message  (u/sprintf "(nil? %s) => %s" (pr-str actual) (pr-str result))
      :expected expected-form
      :actual   actual
      :matcher  'accept-nil}]))


#_{:clj-kondo/ignore [:dynamic-var-not-earmuffed]}
(def ^:dynamic default-timeout 5000)


#?(:clj 
   (defn accept-delay [expected-value expected-form delayed path] 
     (let [actual (deref delayed default-timeout ::timeout)] 
       (if (not= actual ::timeout)
         (accept? expected-value
                  expected-form
                  actual
                  path)
         [{:path     path
           :type     :fail
           :message  "timeout"
           :expected expected-form
           :actual   "timeout"
           :matcher  'accept-delay}]))))


;;
;; Extend ExtendedEquality to common cases:
;;


(extend-protocol ExtendedEquality
  #?(:clj clojure.lang.IPersistentCollection
     :cljs cljs.core/ICollection)
  (accept? [expected-value expected-form actual path]
    (accept-collection expected-value expected-form actual path))

  ; TODO: Why above extend of cljs.core/ICollection is not enough to 
  ; match simple `{}`, `[]`, `#{}`, or `()` etc?
  #?@(:cljs
      (cljs.core/PersistentHashMap
       (accept? [expected-value expected-form actual path] (accept-collection expected-value expected-form actual path))

       cljs.core/PersistentArrayMap
       (accept? [expected-value expected-form actual path] (accept-collection expected-value expected-form actual path))

       cljs.core/PersistentTreeMap
       (accept? [expected-value expected-form actual path] (accept-collection expected-value expected-form actual path))

       cljs.core/PersistentHashSet
       (accept? [expected-value expected-form actual path] (accept-collection expected-value expected-form actual path))

       cljs.core/PersistentTreeSet
       (accept? [expected-value expected-form actual path] (accept-collection expected-value expected-form actual path))

       cljs.core/PersistentVector
       (accept? [expected-value expected-form actual path] (accept-collection expected-value expected-form actual path))

       cljs.core/List
       (accept? [expected-value expected-form actual path] (accept-collection expected-value expected-form actual path))

       cljs.core/Cons
       (accept? [expected-value expected-form actual path] (accept-collection expected-value expected-form actual path))

       cljs.core/LazySeq
       (accept? [expected-value expected-form actual path] (accept-collection expected-value expected-form actual path))))

  #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)
  (accept? [expected-value expected-form actual path]
    (accept-equals expected-value expected-form actual path))

  #?(:clj clojure.lang.Symbol :cljs cljs.core/Symbol)
  (accept? [expected-value expected-form actual path]
    (accept-equals expected-value expected-form actual path))

  #?(:clj clojure.lang.IFn :cljs function)
  (accept? [expected-value expected-form actual path]
    (accept-fn expected-value expected-form actual path))

  #?(:clj java.util.regex.Pattern :cljs js/RegExp)
  (accept? [expected-value expected-form actual path]
    (accept-re expected-value expected-form actual path))

  #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo)
  (accept? [expected-value expected-form actual path]
    (accept-ex-info expected-value expected-form actual path))

  #?(:clj java.lang.Throwable :cljs js/Error)
  (accept? [expected-value expected-form actual path]
    (accept-throwable expected-value expected-form actual path))

  nil
  (accept? [expected-value expected-form actual path]
    (accept-nil expected-value expected-form actual path))

  #?@(:clj
      (java.lang.Class
       (accept? [expected-value expected-form actual path]
                (accept-class expected-value expected-form actual path))

       clojure.lang.IDeref
       (accept? [expected-value expected-form actual path]
                (accept-delay expected-value expected-form actual path))

       java.lang.Object
       (accept? [expected-value expected-form actual path]
                (accept-equals expected-value expected-form actual path)))

      :cljs
      (string
       (accept? [expected-value expected-form actual path]
                (accept-equals expected-value expected-form actual path))

       number
       (accept? [expected-value expected-form actual path]
                (accept-equals expected-value expected-form actual path))

       boolean
       (accept? [expected-value expected-form actual path]
                (accept-equals expected-value expected-form actual path)))))
