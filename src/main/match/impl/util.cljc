(ns match.impl.util
  #?(:cljs (:require [goog.object :as go]
                     [goog.string.format]
                     [goog.string :as gs])))


;;
;; Cross-compatibility utils:
;;


(defn class-name [c]
  #?(:clj (when c (.getName ^Object c))
     :cljs (when c (or (go/get c "name") "<unknown>"))))


(defn exception-class-name [e]
  #?(:clj (some-> ^Object e (.getClass) (class-name))
     :cljs (class-name e)))


(defn exception-message [e]
  #?(:clj (some-> ^Throwable e (.getMessage) (pr-str))
     :cljs (when e (go/get e "message"))))


(defn sprintf [fmt & args]
  #?(:clj (apply format fmt args)
     :cljs (apply gs/format fmt args)))
