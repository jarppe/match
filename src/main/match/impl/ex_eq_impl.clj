(ns match.impl.ex-eq-impl
  (:require [match.impl.ex-eq :as exeq]
            [match.impl.accepts :as a]))

(extend-protocol exeq/ExtendedEquality
  clojure.lang.IPersistentMap
  (accept? [expected-value expected-form actual path]
    (a/accept-associative expected-value expected-form actual path))

  clojure.lang.IPersistentVector
  (accept? [expected-value expected-form actual path]
    (a/accept-sequential expected-value expected-form actual path))

  clojure.lang.APersistentSet
  (accept? [expected-value expected-form actual path]
    (a/accept-set expected-value expected-form actual path))

  clojure.lang.Keyword
  (accept? [expected-value expected-form actual path]
    (a/accept-equals expected-value expected-form actual path))

  clojure.lang.IFn
  (accept? [expected-value expected-form actual path]
    (a/accept-fn expected-value expected-form actual path))

  java.util.regex.Pattern
  (accept? [expected-value expected-form actual path]
    (a/accept-re expected-value expected-form actual path))

  java.lang.Class
  (accept? [expected-value expected-form actual path]
    (a/accept-class expected-value expected-form actual path))

  clojure.lang.ExceptionInfo
  (accept? [expected-value expected-form actual path]
    (a/accept-ex-info expected-value expected-form actual path))

  java.lang.Throwable
  (accept? [expected-value expected-form actual path]
    (a/accept-throwable expected-value expected-form actual path))

  nil
  (accept? [expected-value expected-form actual path]
    (a/accept-nil expected-value expected-form actual path))

  java.lang.Object
  (accept? [expected-value expected-form actual path]
    (a/accept-equals expected-value expected-form actual path)))
