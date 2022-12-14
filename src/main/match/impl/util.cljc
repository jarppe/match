(ns match.impl.util
  #?(:cljs (:require [goog.object :as go]
                     [goog.string.format]
                     [goog.string :as gs])))


;;
;; Cross-compatibility utils:
;;


(defn exception-class-name [c]
  #?(:clj (when c (.getName (.getClass ^Throwable c)))
     :cljs (when c (or (go/get c "name") "<unknown>"))))


(defn exception-message [e]
  #?(:clj (when e (.getMessage ^Throwable e))
     :cljs (when e (go/get e "message"))))


(defn sprintf [fmt & args]
  #?(:clj (apply format fmt args)
     :cljs (apply gs/format fmt args)))
