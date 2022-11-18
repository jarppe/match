(ns match.core
  (:require [clojure.test]
            [match.impl.assert-expr]
            [match.impl.ex-eq :as exeq]
            [match.impl.ex-eq-impl])
  #?(:cljs (:require-macros [match.async])))


(defn matches?
  "Returns a seq of failing test result, where each result is a map having:
   
     `:type`     either `:fail` or `:error`
     `:path`     vector indicating the path to tested value
     `:message`  string message of the test
   
   If all tests pass returns `nil`.

   Example:
   ```
   (->> (matches? {:foo {:bar #{\"a\" \"b\"}}}
                  {:foo {:bar \"c\"
                         :boz :biz}})
        (filter (comp (partial not= :pass) :type)))
   ; => ({:type    :fail
   ;      :path    [:foo :bar],
   ;      :message \"(contains? nil \\\"c\\\") => false\", :expected nil, :actual \\\"c\\\"})
   ```
   
   This should not be called directly in testing. This can be called in REPL to inspect matching, 
   but `clojure.test` tests do not call this. Instead, tests call `match.impl.run/run` via the 
   multi-method at `clojure.test/assert-expr`."
  [expected actual]
  (->> (exeq/accept? expected "" actual [])
       (filter (comp (complement #{:pass}) :type))
       (seq)))


(comment
  (matches? {:foo {:bar #{"a" "b"}}}
            {:foo {:bar "c"
                   :boz :biz}})
  ;; => ({:type    :fail
  ;;      :path    [:foo :bar],
  ;;      :message "(contains? nil \"c\") => false", :expected nil, :actual "c"})
  )
