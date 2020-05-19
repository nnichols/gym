(ns gym.graph-theory.infection
  (:require [clojurewerkz.statistiker.statistics :as stats]))

(def transmission-probabilities
  {:test      0.5
   :raw       0.1
   :wash      0.05
   :distance  0.01
   :isolation 0})

(def k-8-graph
  {:a {:edges     [:b :c :d :e :f :g :h]
       :infected? true}
   :b {:edges [:a :c :d :e :f :g :h]
       :infected? false}
   :c {:edges [:a :b :d :e :f :g :h]
       :infected? false}
   :d {:edges [:a :b :c :e :f :g :h]
       :infected? false}
   :e {:edges [:a :b :c :d :f :g :h]
       :infected? false}
   :f {:edges [:a :b :c :d :e :g :h]
       :infected? false}
   :g {:edges [:a :b :c :d :e :f :h]
       :infected? false}
   :h {:edges [:a :b :c :d :e :f :g]
       :infected? false}})

(defn all-infected?
  [graph]
  (every? :infected? (vals graph)))

(defn becomes-infected?
  [node graph transmission-probability]
  (let [edges (get-in graph [node :edges])
        infected-edges (count (filter #(true? (get-in graph [% :infected?])) edges))
        safety-chance (Math/pow (- 1 transmission-probability) infected-edges)
        infection-roll (rand)]
    (>= infection-roll safety-chance)))

(defn spread-infection
  [graph transmission-probability]
  (let [reducing-fn (fn [m k v] (if (:infected? v)
                                  (assoc m k v)
                                  (assoc m k (assoc v :infected? (becomes-infected? k graph transmission-probability)))))]
    (reduce-kv reducing-fn {} graph)))

(defn simulate-outbreak!
  [base-graph transmission-rate max-generations]
    (loop [graph      base-graph
           generation 1]
      (if (or (all-infected? graph) (>= generation max-generations))
        generation
        (recur (spread-infection graph transmission-rate) (inc generation)))))

(defn average-time-to-saturation
  [base-graph opts]
  (let [transmission-rate (get transmission-probabilities (:transmission opts) 0.1)
        max-generations   (or (:generations opts) 10000)
        trials            (or (:trials opts) 10000)
        generations       (repeatedly trials #(simulate-outbreak! base-graph transmission-rate max-generations))
        successful-runs   (count (filter #(= max-generations %) generations))
        average           (stats/mean generations)
        deviation         (stats/standard-deviation generations)
        median            (stats/median generations)]
    (format "Transmission rate: %s | Trials: %s | Successful Runs: %s | Average: %s | Standard Deviation: %s | Median: %s"
            transmission-rate
            trials
            successful-runs
            average
            deviation
            median)))
            
(average-time-to-saturation k-8-graph {:transmission :distance :trials 100000 :generations 90})
