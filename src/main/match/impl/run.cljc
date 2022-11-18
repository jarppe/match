(ns match.impl.run
  (:require [clojure.string :as str]
            [match.impl.ex-eq :as exeq]
            [match.impl.util :as u]))


(defn run [msg [_ expected-form actual-form]]
  `(test/report
    (let [msg# ~msg
          actual# (try
                    ~actual-form
                    (catch js/Object e#
                      e#))
          results# (exeq/accept? ~expected-form '~expected-form actual# [])
          status# (if (every? (comp (partial = :pass) :type) results#)
                    :pass
                    :fail)
          message# (->> results#
                        (filter (comp (partial not= :pass) :type))
                        (map-indexed (fn [n# result#]
                                       (u/sprintf "#%d at %s:\n  %s"
                                                  n#
                                                  (-> result# :path (pr-str))
                                                  (-> result# :message))))
                        (str/join "\n"))]
      {:type status#
       :message (let [msg# ~msg]
                  (if-not (str/blank? msg#)
                    (str msg# ":\n" message#)
                    message#))
       :expected ~expected-form
       :actual actual#})))
