(ns example.get-users
  (:require [clj-http.client :as http]))


(defn get-user-todos [user-id]
  (http/get (str "https://jsonplaceholder.typicode.com/users/" user-id "/todos")
            {:accept :json
             :as     :json}))

