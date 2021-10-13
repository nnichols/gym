(ns gym.brew-bot
  (:require [brew-bot.core :as bb]))

(defn random-fermentables
  []
  (bb/select-fermentables :random {:count-cutoff 3
                                   :amount-cutoff 4
                                   :include-adjuncts? false
                                   :include-sugars? false}))

(defn random-hops
  []
  (bb/select-hops :random {:count-cutoff 2
                           :amount-cutoff 0.05}))

(defn random-yeasts
  []
  (bb/select-yeasts :random {:count-cutoff 1
                             :amount-cutoff 0.5
                             :default-weight 1
                             :include-dcl-fermentis? false}))

(defn my-random-recipe
  []
  (bb/ingredients->cbf-recipe-template (random-fermentables) (random-hops) (random-yeasts)))

(defn weighted-fermentables
  []
  (bb/select-fermentables :weighted {:count-cutoff      3
                                     :amount-cutoff     4
                                     :default-weight    0.0005
                                     :include-adjuncts? false
                                     :include-sugars?   false
                                     :selection-weight  {:barley-flaked      2.0
                                                         :cara-pils-dextrine 50.0
                                                         :munich-malt        1000.0}}))

(defn another-recipe
  []
  (bb/ingredients->cbf-recipe-template (weighted-fermentables) (random-hops) (random-yeasts)))
