(ns match.async)


(defn compiling-cljs? []
  (boolean
   (when-let [n (find-ns 'cljs.analyzer)]
     (when-let [v (ns-resolve n '*cljs-file*)]
       @v))))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro matches-async? [expected-form actual-form]
  (if (compiling-cljs?)
    `(cljs.test/async ~'done (-> (js/Promise.resolve nil)
                                 (.then (fn [~'_]
                                          (try
                                            ~actual-form
                                            (catch ~'js/Object ~'e
                                              ~'e))))
                                 (.then (fn [~'actual]
                                          (~'is (~'matches? ~expected-form ~'actual))))
                                 (.catch (fn [~'e]
                                           (throw (ex-info "unexpected error"
                                                           {:expected-form '~expected-form
                                                            :actual-form   '~actual-form}
                                                           ~'e))))
                                 (.finally ~'done)))
    `(~'is (~'matches? ~expected-form
                       (deref ~actual-form)))))


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
;; (async
;;  ready
;;  (match.impl.async-run-cljs/run
;;   ready
;;   '(some expected form)
;;   '(the actual form)))

;; CLJ:
;; (is (matches? (some expected form) (the actual form)))

  ;
  )