(ns gym.beer.send
  (:require [again.core :as again]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [keg.core :as keg]
            [taoensso.timbre :as timbre]))

(def  WEBHOOK_URL (System/getenv "WEBHOOK_URL"))

(defn send-recipe!
  [r recipe-id line-num]
  (try
    (again/with-retries
      [1000 12000 24000]
      (timbre/infof "Posting %s to Discord" (:name r))
      (let [message (format "Successfully fetched: %s (%s of 30562) - https://www.brewersfriend.com/homebrew/recipe/view/%s" (:name r) line-num recipe-id)
            body    (json/generate-string {:user-name "brew-bot"
                                           :content   message})
            resp    (http/post WEBHOOK_URL {:content-type :json
                                            :as           :json
                                            :body         body})]
        (if (= 204 (:status resp))
          ::ok
          ::failure)))
    (catch Throwable t
      (timbre/error t "Failed to post to Discord!")
      ::failure)))

(keg/tap #'send-recipe! keg/pour-runtime)
