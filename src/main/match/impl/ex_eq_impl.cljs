(ns match.impl.ex-eq-impl
  (:require [match.impl.ex-eq :as exeq]
            [match.impl.accepts :as a]))


(extend-protocol exeq/ExtendedEquality
  cljs.core/PersistentHashMap
  (accept? [expected-value expected-form actual path]
    (a/accept-associative expected-value expected-form actual path))

  cljs.core/PersistentArrayMap
  (accept? [expected-value expected-form actual path]
    (a/accept-associative expected-value expected-form actual path))

  cljs.core/PersistentTreeMap
  (accept? [expected-value expected-form actual path]
    (a/accept-associative expected-value expected-form actual path))

  cljs.core/PersistentVector
  (accept? [expected-value expected-form actual path]
    (a/accept-sequential expected-value expected-form actual path))

  cljs.core/PersistentHashSet
  (accept? [expected-value expected-form actual path]
    (a/accept-set expected-value expected-form actual path))

  cljs.core/PersistentTreeSet
  (accept? [expected-value expected-form actual path]
    (a/accept-set expected-value expected-form actual path))

  cljs.core/Keyword
  (accept? [expected-value expected-form actual path]
    (a/accept-equals expected-value expected-form actual path))

  function
  (accept? [expected-value expected-form actual path]
    (a/accept-fn expected-value expected-form actual path))

  js/RegExp
  (accept? [expected-value expected-form actual path]
    (a/accept-re expected-value expected-form actual path))

  cljs.core/ExceptionInfo
  (accept? [expected-value expected-form actual path]
    (a/accept-ex-info expected-value expected-form actual path))

  js/Error
  (accept? [expected-value expected-form actual path]
    (a/accept-throwable expected-value expected-form actual path))

  nil
  (accept? [expected-value expected-form actual path]
    (a/accept-nil expected-value expected-form actual path))

  string
  (accept? [expected-value expected-form actual path]
    (a/accept-equals expected-value expected-form actual path))

  number
  (accept? [expected-value expected-form actual path]
    (a/accept-equals expected-value expected-form actual path))

  boolean
  (accept? [expected-value expected-form actual path]
    (a/accept-equals expected-value expected-form actual path)))
