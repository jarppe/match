(ns match.impl.run
  (:require [clojure.string :as str]
            [match.impl.exeq :as exeq]
            [match.impl.util :as u]))


(defn run [env-type msg [_ expected-form actual-form]]
  (let [ExceptionType (case env-type
                        :clj 'java.lang.Throwable
                        :cljs :default)]
    `(clojure.test/report
      (let [expected# (try
                        ~expected-form
                        (catch ~ExceptionType ~'e
                          (throw (ex-info "exception while evaluating the `expected-form`"
                                          {:expected-form '~expected-form
                                           :exception     ~'e}
                                          ~'e))))
            actual#   (try
                        ~actual-form
                        (catch ~ExceptionType ~'e
                          ~'e))
            results#  (exeq/accept? expected# '~expected-form actual# [])
            status#   (if (every? (comp #{:pass} :type) results#)
                        :pass
                        :fail)
            message#  (->> results#
                           (filter (comp (complement #{:pass}) :type))
                           (map-indexed (fn [n# result#]
                                          (u/sprintf "#%d at %s:\n  %s"
                                                     n#
                                                     (-> result# :path (pr-str))
                                                     (-> result# :message))))
                           (str/join "\n"))
            expected# (if (-> ~expected-form
                              (pr-str)
                              (count)
                              (< 50))
                        ~expected-form
                        "truncated...")
            actual#   (if (-> actual#
                              (pr-str)
                              (count)
                              (< 50))
                        actual#
                        "truncated...")]
        {:type     status#
         :message  (let [msg# ~msg]
                     (if-not (str/blank? msg#)
                       (str msg# ":\n" message#)
                       message#))
         :expected expected#
         :actual   actual#}))))
