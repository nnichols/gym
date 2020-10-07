(ns gym.beer-xml.ingredient-scrape
  (:require [pl.danieljanus.tagsoup :as ts]
            [clojure.string :as cs])) ;; XML is truly the worst

(defn parse-fermentable
  [url]
  (ts/parse url))
