(ns match.exeq-test
  (:require [clojure.test :as test :refer [deftest testing is]]
            [match.impl.exeq :refer [accept?]]
            #?(:cljs [goog.object :as go])))


; Ideally this would take just the expected-form and eval it to
; get expected-value, but I can't get eval to work on cljs:

(defn failures [expected-value expected-form actual]
  (->> (accept? expected-value expected-form actual [])
       (filter (comp (complement #{:pass}) :type))))


(deftest map-test
  (testing "matching against a map"
    (is (= (failures {:foo :bar} '{:foo :bar} {:foo :bar})
           []))
    (is (= (failures {:foo :bar} '{:foo :bar} nil)
           [{:type     :fail
             :path     []
             :expected {:foo :bar}
             :actual   nil
             :message  "(map? nil) => false"
             :matcher  'accept-associative}]))
    (is (= (failures {:foo :bar} '{:foo :bar} {:foo :baz})
           [{:type     :fail
             :path     [:foo]
             :expected :bar
             :actual   :baz
             :message  "(= :bar :baz) => false"
             :matcher  'accept-equals}]))))


(deftest vector-test
  (testing "matching against a vector"
    (is (= (failures [:a :b :c] '[:a :b :c] [:a :b :c])
           []))
    (is (= (failures [:a :b :c] '[:a :b :c] nil)
           [{:type     :fail
             :path     []
             :expected [:a :b :c]
             :actual   nil
             :message  "(sequential? nil) => false"
             :matcher  'accept-sequential}]))
    (is (= (failures [:a :b :c] '[:a :b :c] [:a :x :c])
           [{:type     :fail
             :path     [1]
             :expected :b
             :actual   :x
             :message  "(= :b :x) => false"
             :matcher  'accept-equals}]))
    (is (= (failures [:a :b :c] '[:a :b :c] [:a :b])
           [{:type     :fail
             :path     [2]
             :expected :c
             :actual   nil
             :message  "expected more than 2 elements"
             :matcher  'accept-sequential}]))
    (is (= (failures [:a :b :c] '[:a :b :c] [:a :b :c :d])
           [{:type     :fail
             :path     [3]
             :expected nil
             :actual   :d
             :message  "did not expect more than 3 elements"
             :matcher  'accept-sequential}])))
  (testing "matching against a vector allowing extra values"
    (is (= (failures [:a :b :c :...] '[:a :b :c :...] [:a :b :c])
           []))
    (is (= (failures [:a :b :c :...] '[:a :b :c :...] [:a :b :c :d :e])
           [])))
  (testing "matching against a vector allowing extra values, using symbol"
    (is (= (failures [:a :b :c '...] '[:a :b :c '...] [:a :b :c])
           []))
    (is (= (failures [:a :b :c '...] '[:a :b :c '...] [:a :b :c :d :e])
           []))))


; These are delegated to accept-sequential, so the vector-test cases apply.
(deftest sequential-test
  (testing "matching list against list"
    (is (= (failures '(1 2 3) '(1 2 3) `(1 2 3)) [])))
  (testing "matching list against vector"
    (is (= (failures '(1 2 3) '(1 2 3) [1 2 3]) [])))
  (testing "matching list against seq"
    (is (= (failures '(1 2 3) '(1 2 3) (cons 1 [2 3])) [])))
  (testing "matching list against lazy seq"
    (is (= (failures '(1 2 3) '(1 2 3) (map identity [1 2 3])) [])))
  (testing "matching seq against list"
    (is (= (failures (cons 1 [2 3]) '(cons 1 [2 3]) '(1 2 3)) [])))
  (testing "matching lazy seq against list"
    (is (= (failures (map identity [1 2 3]) '(map identity [1 2 3]) '(1 2 3)) []))))


(deftest set-test
  (testing "matching against a set"
    (is (= (failures #{:a :b :c} '#{:a :b :c} :a)
           []))
    (is (= (failures #{:a :b :c} '#{:a :b :c} nil)
           [{:type     :fail
             :path     []
             :expected #{:a :b :c}
             :actual   nil
             :message  "(contains? #{:c :b :a} nil) => false"
             :matcher  'accept-set}]))
    (is (= (failures #{:a :b :c} '#{:a :b :c} :x)
           [{:type     :fail
             :path     []
             :expected #{:a :b :c}
             :actual   :x
             :message  "(contains? #{:c :b :a} :x) => false"
             :matcher  'accept-set}]))))


(deftest keyword-test
  (testing "matching against a keyword"
    (is (= (failures :a ':a :a)
           []))
    (is (= (failures :a ':a nil)
           [{:type     :fail
             :path     []
             :expected :a
             :actual   nil
             :message  "(= :a nil) => false"
             :matcher  'accept-equals}]))
    (is (= (failures :a ':a :x)
           [{:type     :fail
             :path     []
             :expected :a
             :actual   :x
             :message  "(= :a :x) => false"
             :matcher  'accept-equals}]))
    (is (= (failures :a ':a 'a)
           [{:type     :fail
             :path     []
             :expected :a
             :actual   'a
             :message  "(= :a a) => false"
             :matcher  'accept-equals}]))))


(deftest symbol-test
  (testing "matching against a symbol"
    (is (= (failures 'a 'a 'a)
           []))
    (is (= (failures 'a 'a nil)
           [{:type     :fail
             :path     []
             :expected 'a
             :actual   nil
             :message  "(= a nil) => false"
             :matcher  'accept-equals}]))
    (is (= (failures 'a 'a 'x)
           [{:type     :fail
             :path     []
             :expected 'a
             :actual   'x
             :message  "(= a x) => false"
             :matcher  'accept-equals}]))
    (is (= (failures 'a 'a :a)
           [{:type     :fail
             :path     []
             :expected 'a
             :actual   :a
             :message  "(= a :a) => false"
             :matcher  'accept-equals}]))))


(deftest fn-test
  (testing "matching against a fn"
    (is (= (failures (partial = :a) '(partial = :a) :a)
           []))
    (is (= (failures (partial = :a) '(partial = :a) nil)
           [{:type     :fail
             :path     []
             :expected '(partial = :a)
             :actual   nil
             :message  "((partial = :a) nil) => false"
             :matcher  'accept-fn}]))
    (is (= (failures (partial = :a) '(partial = :a) :x)
           [{:type     :fail
             :path     []
             :expected '(partial = :a)
             :actual   :x
             :message  "((partial = :a) :x) => false"
             :matcher  'accept-fn}]))))


(deftest regex-test
  (testing "matching against a regex"
    (is (= (failures #"Sisyphus" '#"Sisyphus" "One must imagine Sisyphus happy")
           []))
    (is (= (failures #"^Sisyphus" '#"^Sisyphus" "Sisyphus happy")
           []))
    (is (= (failures #"Sisyphus$" '#"Sisyphus$" "One must imagine Sisyphus")
           []))
    (is (= (failures #"^Sisyphus$" '#"^Sisyphus$" "Sisyphus")
           []))
    ; Regex equality is difficult to test, as `(= #"Sisyphus" #"Sisyphus") => false`  
    ; More over, in clj  (str #"Sisyphus") => "Sisyphus"
    ;            in cljs (str #"Sisyphus") => "/Sisyphus/"
    (is (= (->> (failures #"Sisyphus" '#"^Sisyphus$" nil)
                (map #(dissoc % :expected)))
           [{:type    :fail
             :path    []
             :actual  nil
             :message "(re-find #\"^Sisyphus$\" nil) => false"
             :matcher 'accept-re}]))
    (is (= (->> (failures #"Sisyphus" '#"Sisyphus" "One must imagine everyone happy")
                (map #(dissoc % :expected)))
           [{:type    :fail
             :path    []
             :actual  "One must imagine everyone happy"
             :message "(re-find #\"Sisyphus\" \"One must imagine everyone happy\") => nil"
             :matcher 'accept-re}]))))


(deftest nil-test
  (testing "matching against nil"
    (is (= (failures nil 'nil nil)
           []))
    (is (= (failures nil 'nil "Hi")
           [{:type     :fail
             :path     []
             :expected 'nil
             :actual   "Hi"
             :message  "(nil? \"Hi\") => false"
             :matcher  'accept-nil}]))))


(deftest object-test
  (testing "matching against native types"
    (is (= (failures "hi" "hi" "hi")
           []))
    (is (= (failures "hi" "hi" "no")
           [{:type     :fail
             :path     []
             :expected "hi"
             :actual   "no"
             :message  "(= \"hi\" \"no\") => false"
             :matcher  'accept-equals}]))
    (is (= (failures 42 42 42)
           []))
    (is (= (failures 42 42 1337)
           [{:type     :fail
             :path     []
             :expected 42
             :actual   1337
             :message  "(= 42 1337) => false"
             :matcher  'accept-equals}]))
    (is (= (failures true true true)
           []))
    (is (= (failures true true false)
           [{:type     :fail
             :path     []
             :expected true
             :actual   false
             :message  "(= true false) => false"
             :matcher  'accept-equals}]))
    (is (= (failures false false false)
           []))
    (is (= (failures false false true)
           [{:type     :fail
             :path     []
             :expected false
             :actual   true
             :message  "(= false true) => false"
             :matcher  'accept-equals}]))))


(deftest multiple-fails-test
  (is (= (failures {:foo :fofo
                    :bar 42
                    :boz true}
                   '{:foo :fofo
                     :bar 42
                     :boz true}
                   {:foo :fifi
                    :bar 1337
                    :boz false})
         [{:type     :fail
           :path     [:foo]
           :expected :fofo
           :actual   :fifi
           :message  "(= :fofo :fifi) => false"
           :matcher  'accept-equals}
          {:type     :fail
           :path     [:bar]
           :expected 42
           :actual   1337
           :message  "(= 42 1337) => false"
           :matcher  'accept-equals}
          {:type     :fail
           :path     [:boz]
           :expected true
           :actual   false
           :message  "(= true false) => false"
           :matcher  'accept-equals}])))


(deftest nesting-test
  (is (= (failures {:foo {:bar [:a {:b 42} :c]}}
                   '{:foo {:bar [:a {:b 42} :c]}}
                   {:foo {:bar [:a {:b 42} :c]}})
         []))
  (is (= (failures {:foo {:bar [:a {:b #(> % 41)} :c]}}
                   '{:foo {:bar [:a {:b #(> % 41)} :c]}}
                   {:foo {:bar [:a {:b 42} :c]}})
         []))
  (is (= (->> (failures {:foo {:bar [:a {:b #(> % 41)} :c]}}
                        '{:foo {:bar [:a {:b #(> % 41)} :c]}}
                        {:foo {:bar [:a {:b 12} :c]}})
              (map #(dissoc % :message :expected)))
         [{:type    :fail
           :path    [:foo :bar 1 :b]
           :actual  12
           :matcher 'accept-fn}]))
  (testing "check that the message shows function call"
    (is (= "((partial < 42) 12) => false"
           (-> (failures {:b (partial < 42)}
                         '{:b (partial < 42)}
                         {:b 12})
               (first)
               :message)))
    ; The message is of form:
    ; "((fn* [p1__45530#] (> p1__45530# 41)) 12) => false"
    (is (= true
           (->> (failures {:foo {:bar [:a {:b #(> % 41)} :c]}}
                          '{:foo {:bar [:a {:b #(> % 41)} :c]}}
                          {:foo {:bar [:a {:b 12} :c]}})
                (first)
                :message
                (re-matches #"\(\(fn\* \[[^#]+#\] \(> [^#]+# 41\)\) 12\) => false")
                (some?))))))


(deftest exception-test
  (testing "expecting an exception"
    (let [expected-form #?(:clj '(java.io.IOException.) :cljs '(js/SyntaxError.))
          expected      #?(:clj (java.io.IOException.) :cljs (js/SyntaxError.))]
      (is (= (failures expected expected-form #?(:clj (java.io.IOException.) :cljs (js/SyntaxError.)))
             []))
      (is (= (->> (failures expected expected-form #?(:clj (java.lang.ArithmeticException.) :cljs (js/RangeError.)))
                  (map #(dissoc % :actual)))
             [{:type     :fail
               :path     []
               :expected expected-form
               :message  (str "expected instance of "
                              #?(:clj "java.io.IOException" :cljs "SyntaxError")
                              ", but got "
                              #?(:clj "java.lang.ArithmeticException" :cljs "RangeError"))
               :matcher  'accept-throwable}]))))
  (testing "exception message must match too"
    (is (= (->> (failures #?(:clj (java.io.IOException. "Oh no")
                             :cljs (doto (js/SyntaxError.)
                                     (go/set "message" "Oh no")))
                          #?(:clj '(java.io.IOException. "Oh no")
                             :cljs '(js/SyntaxError. "Oh no"))
                          #?(:clj (java.io.IOException. "Not this")
                             :cljs (doto (js/SyntaxError.)
                                     (go/set "message" "Not this"))))
                (map #(dissoc % :actual)))
           [{:type     :fail
             :path     [:message]
             :expected "Oh no"
             :message  "(= \"Oh no\" \"Not this\") => false"
             :matcher  'accept-equals}]))))


(deftest ex-info-test
  (testing "matching against an ex-info"
    (is (= (failures (ex-info "foo" {:a 42})
                     '(ex-info "foo" {:a 42})
                     (ex-info "foo" {:a 42}))
           []))
    (testing "Wrong message"
      (is (= (failures (ex-info "foo" {:a 42})
                       '(ex-info "foo" {:a 42})
                       (ex-info "bar" {:a 42}))
             [{:type     :fail
               :path     [:message]
               :expected "foo"
               :actual   "bar"
               :message  "(= \"foo\" \"bar\") => false"
               :matcher  'accept-equals}])))
    (testing "Wrong data"
      (is (= (failures (ex-info "foo" {:a 42})
                       '(ex-info "foo" {:a 42})
                       (ex-info "foo" {:a 1337}))
             [{:type     :fail
               :path     [:data :a]
               :expected 42
               :actual   1337
               :message  "(= 42 1337) => false"
               :matcher  'accept-equals}])))
    ; Dropping :expected, as comparing exception instances is not portable.
    (testing "Wrong everything"
      (is (= (->> (failures (ex-info "foo" {:a 42})
                            '(ex-info "foo" {:a 42})
                            nil)
                  (map #(dissoc % :expected)))
             [{:type    :fail
               :path    []
               :actual  nil
               :message (str "expected instance of "
                             #?(:clj "clojure.lang.ExceptionInfo"
                                :cljs "cljs.core/ExceptionInfo")
                             ", but got null")
               :matcher 'accept-ex-info}])))))
