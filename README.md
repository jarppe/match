# match - Extended matching for your clojure.test cases

**Warning**: This library is in alpha state, expect breaking changes.

## Quick example

```clj
(deftest simple-example-test
  (let [data {:name    "Ada Lovelace"
              :title   "Countess of Lovelace"
              :dob     "10 December 1815"
              :address {:city    "London"
                        :country "UK"}}]
    (is (matches? {:name    string?
                   :dob     #"\d+ \S+ \d{4}"
                   :address {:city #{"Surrey" "Ross-shire" "London"}}
                  data))))
```

The `matches?` verifies that the `data` is an associative data-structure that
contains:

- for key `:name` a value that is a string
- for key `:dob` a string that matches the given regular expressions
- for key `:address` a nested associative data-structure, which in turn contains:
- for key `:city` and that has either `"Surrey"`, `"Ross-shire"`, or `"London"`

`matches?` can report multiple errors from one statement, and each error shows the location
of the error. For example, lets change the `data` above so that we get multiple errors:

```clj
(deftest simple-failing-example-test
  (let [data {:name    :ada-lovelace
              :title   "Countess of Lovelace"
              :address {:city    "Nottinghamshire"
                        :country "UK"}}]
    (is (matches? {:name    string?
                   :dob     #"\d+ \S+ \d{4}"
                   :address {:city #{"Surrey" "Ross-shire" "London"}}}
                  data))))
```

The above test fails with following message:

```
#0 at [:name]:
  (string? :ada-lovelace) => false
#1 at [:dob]:
  actual is missing key :dob
#2 at [:address :city]:
  (contains? #{"Surrey" "Ross-shire" "London"} "Nottinghamshire") => false
```

`match` works with `Clojure` and `ClojureScript`.

## More concrete examples

How to test following function that fetches some users, and returns
the HTTP response?

```clj
(ns example.get-users
  (:require [clj-http.client :as http]))


(defn get-user-todos [user-id]
  (http/get (str "https://jsonplaceholder.typicode.com/users/" user-id "/todos")
            {:accept :json
             :as     :json}))
```

You would like to test that the response status is `200`, the content-type is
JSON with optional charset, and that the body contains TODO's that all belong to
user `1`.

You could call this function in your test, extract data from response, and finally
test each data element separately.

```clj
(deftest traditional-test
  (let [resp (users/get-user-todos 1)]
    (is (= 200 (:status resp)))
    (is (str/starts-with? (get-in resp [:headers "Content-Type"]) "application/json"))
    (is (every? (comp (partial = 1) :userId) (:body resp)))))
```

With this library, you can test everything is one go:

```clj
(deftest get-user-todos-test
  (testing "Ensure that all TODO's belong to user 1"
    (is (matches? {:status  200
                   :headers {"Content-Type" #(str/starts-with? % "application/json")}
                   :body    (fn [body]
                              (every? (comp (partial = 1) :userId) body))}
                  (users/get-user-todos 1)))))
```

The `maches?` checks that the response contains _at least_ the `:status`, `:headers`,
and `:body`. Also, it checks that the `:status` is `200`, the `:headers` contains
_at least_ `"Content-Type"` and that the content-type starts with `"application/json"`,
and finally, that the body matches the given predicate.

Here's another example test:

```clj
(deftest nested-data-test
  (is (matches? {:foo {:bar {:message "hello"}
                       :biz [{:a 1}
                             {:b 2}
                             :...]}}
                {:foo {:bar {:message "world"}
                       :biz [{:a 1}
                             {:b 3}
                             {:c 4}]}})))
```

The above test fails with following errors:

```
FAIL in (nested-data-test) (:)
#0 at [:foo :bar :message]:
   (= "hello" "world") => false
#1 at [:foo :biz 1 :b]:
   (= 2 3) => false
expected: {:foo {:bar {:message "hello"}, :biz [{:a 1} {:b 2} :...]}}
actual: {:foo {:bar {:message "world"}, :biz [{:a 1} {:b 3} {:c 4}]}}
```

The above shows that we have multiple problems. First, at the
nested path `[:foo :bar :message]` we expected `"hello"`, but we
got `"world"`.

Second, at path `[:foo :biz 1 :b]` (the number `1` index `at the vector) we expected value`2`but got`3`.

We can fix the test like this:

```clj
(deftest nested-data-test
  (is (matches? {:foo {:bar {:message "hello"}
                       :biz [{:a 1}
                             {:b 2}
                             :...]}}
                {:foo {:bar {:message "hello"}
                       :biz [{:a 1}
                             {:b 2}
                             {:c 4}]}})))
```

Note that at `[:foo :biz]` we expect a sequential data with at least
to elements, first being `{:a 1}` and second `{:b 2}`. The special
keyword `:...` means _and the optionally more elements_.

Lets see what happens without the `:...`:

```clj
(deftest nested-data-test
  (is (matches? {:foo {:bar {:message "hello"}
                       :biz [{:a 1}
                             {:b 2}]}}
                {:foo {:bar {:message "hello"}
                       :biz [{:a 1}
                             {:b 2}
                             {:c 4}]}})))
```

We get an error:

```
FAIL in (nested-data-test) (:)
#0 at [:foo :biz 2]:
  did not expect more than 2 elements
expected: {:foo {:bar {:message "hello"}, :biz [{:a 1} {:b 2}]}}
  actual: {:foo {:bar {:message "hello"}, :biz [{:a 1} {:b 2} {:c 4}]}}
4 tests, 6 assertions, 1 failures.
```

The above error shows that at `[:foo :biz 2]` we expected only 2 values, but the
actual value had more.

## Expecting exceptions

When the code under test should throw an exception, simply expect the exception instance.

```clj
(deftest expecting-exception-test
  (is (matches? (ex-info "Permissions denied" {:error "Not allowed"})
                (do (println "Trying something nefarious...")
                    (throw (ex-info "Permissions denied" {:error "Not allowed"
                                                          :id    123})))))
  (is (matches? (java.io.IOException. "File is gone")
                (do (println "Opening file...")
                    (throw (java.io.IOException. "File is gone"))))))
```

`matches?` checks that the exception is of right type and that the message
matches, if one is provided.

The `ex-info` data is checked using the same rules as any map.

## Extended equality

The `matches?` compares the expected and actual using "extended equality" tests. From above
examples we already was that if the expected value is a map, `matches?` uses the keys and
values of the map to perform nested checks.

Extended equality is implemented by a protocol [match.impl.exeq/ExtendedEquality](src/main/match/impl/exeq.cljc#L28). The protocol is extended to most common data types, but you can
extend it your specific needs.

## Async tests

_TODO_ Needs documentation.
