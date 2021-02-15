(ns gym.github
  (:require [clj-http.client :as http]))

(def github-token
 (System/getenv "GITHUB_API_TOKEN"))

(def github-uri "https://api.github.com")

(defn get-repo-webhooks
  [owner repo]
  (let [route (str github-uri "/repos/" owner "/" repo "/hooks")
        headers {:Authorization github-token}]
    (http/get route {:headers headers})))
