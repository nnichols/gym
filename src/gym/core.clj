(ns gym.core
  (:require [gym.beer-xml.scrape :as beerx]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println (beerx/try-me!)))
