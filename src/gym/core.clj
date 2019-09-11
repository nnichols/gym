(ns gym.core
  (:require [clj-memory-meter.core :as mm]
            [gym.beer-xml.scrape :as beerx]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println (beerx/try-me!)))
