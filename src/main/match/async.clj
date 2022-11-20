(ns match.async
  (:require [match.core]))


(defn- compiling-cljs? []
  (boolean
   (when-let [n (find-ns 'cljs.analyzer)]
     (when-let [v (ns-resolve n '*cljs-file*)]
       @v))))


(defmacro matches-async? [expected-form actual-form]
  (if (compiling-cljs?)
    `(cljs.test/async ~'done (-> (js/Promise.resolve nil)
                                 (.then (fn [~'_]
                                          (try
                                            ~actual-form
                                            (catch ~'js/Object ~'e
                                              ~'e))))
                                 (.then (fn [~'actual]
                                          (~'cljs.test/is (~'matches? ~expected-form ~'actual))))
                                 (.catch (fn [~'e]
                                           (throw (ex-info "unexpected error"
                                                           {:expected-form '~expected-form
                                                            :actual-form   '~actual-form}
                                                           ~'e))))
                                 (.finally ~'done)))
    `(~'clojure.test/is (~'matches? ~expected-form (deref ~actual-form)))))


(comment

  (compiling-cljs?)

  (require 'clojure.pprint)

  (let [form '(matches-async? (some expected form)
                              (the actual form))]
    (println "CLJS:")
    (with-redefs [compiling-cljs? (constantly true)]
      (-> (macroexpand-1 form)
          (clojure.pprint/pprint)))
    (println "\nCLJ:")
    (with-redefs [compiling-cljs? (constantly false)]
      (-> (macroexpand-1 form)
          (clojure.pprint/pprint))))
;; prints:
;;
;; CLJS:
  `(cljs.test/async
    done
    (clojure.core/->
     (js/Promise.resolve nil)
     (.then
      (clojure.core/fn [_] (try (the actual form) (catch js/Object e e))))
     (.then
      (clojure.core/fn
        [actual]
        (is (matches? (some expected form) actual))))
     (.catch
      (clojure.core/fn
        [e]
        (throw
         (clojure.core/ex-info
          "unexpected error"
          {:actual-form   '(the actual form)
           :expected-form '(some expected form)}
          e))))
     (.finally done)))
;; CLJ:
;; (is (matches? (some expected form) (the actual form)))

  ;
  )