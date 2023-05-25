(ns gym.beer.core
  (:require [clojure.java.io :as io]
            [gym.beer.save :as save.beer]
            [gym.beer.scrape :as scrape.beer]
            [gym.beer.send :as send.beer]
            [keg.core :as keg]
            [taoensso.timbre :as timbre]
            [wb-metrics.logging :as metrics])
  (:gen-class))

(def ctr (atom 0))

(defn drink-beer!
  [recipe-id]
  (when-let [recipe (scrape.beer/fetch-and-normalize! recipe-id)]
    (send.beer/send-recipe! recipe recipe-id @ctr)
    (save.beer/save-recipe! recipe)))

(keg/tap #'drink-beer! keg/pour-runtime-and-args)

(defn stem->id
  [uri-stem]
  (first (re-seq #"\d+" uri-stem)))

(defn steal-beer!
  [file]
  (with-open [rdr (io/reader file)]
    (doseq [line (line-seq rdr)]
      (swap! ctr inc)
      (timbre/infof "Processing line %s: www.brewersfriend.com%s" @ctr line)
      (-> line stem->id drink-beer!))))

(defn -main
  [& args]
  (metrics/configure!)
  (steal-beer! "recipeData.csv"))
