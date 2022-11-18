(ns match.impl.ex-eq)

;;
;; MultiResultResponse:
;; 
;; Type functions can use to return test responses. Used to distinguish test 
;; reports from normal responses.
;;


(defrecord MultiResultResponse [results])


(defn multi-result-response? [response]
  (instance? MultiResultResponse response))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn multi-result-response [results]
  (->MultiResultResponse results))


;;
;; ExtendedEquality
;;


(defprotocol ExtendedEquality
  "Protocol for extended equality"
  (accept? [expected-value expected-form actual path]
    "compare the `expected-value` to `actual` using the extended equality."))
