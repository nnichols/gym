(ns gym.turing.game
  (:require [gym.turing.actions :as actions]))

(defn initialize?
  [_state _signal]
  true)

(defn win?
  [_state {:keys [dice-roll] :as _signal}]
  (and dice-roll (= 7 dice-roll)))

(defn re-roll?
  [_state {:keys [dice-roll] :as _signal}]
  (and dice-roll (= 6 dice-roll)))

(defn lose?
  [_state {:keys [dice-roll] :as _signal}]
  (and dice-roll (not (or (= 6 dice-roll) (= 7 dice-roll)))))

(def states
  {:game-start {:name        "Game started!"
                :transitions [{:name            :->a
                               :match?          #'initialize?
                               :resulting-state :a
                               :action (fn [state _] (update state :a-rolls inc))}]}
   :a          {:name        "Player A's turn"
                :transitions [{:name            :->a-wins
                               :match?          #'win?
                               :resulting-state :a-wins}
                              {:name            :a-reroll
                               :match?          #'re-roll?
                               :resulting-state :a
                               :action (fn [state _] (update state :a-rolls inc))}
                              {:name            :->b
                               :match?          #'lose?
                               :resulting-state :b
                               :action (fn [state _] (update state :b-rolls inc))}]}
   :b          {:name        "Player B's turn"
                :transitions [{:name            :->b-wins
                               :match?          #'win?
                               :resulting-state :b-wins}
                              {:name            :b-reroll
                               :match?          #'re-roll?
                               :resulting-state :b
                               :action (fn [state _] (update state :b-rolls inc))}
                              {:name            :->a
                               :match?          #'lose?
                               :resulting-state :a
                               :action (fn [state _] (update state :a-rolls inc))}]}
   :a-wins     {:name     "Player A wins"}
   :b-wins     {:name     "Player B wins"}})

(-> {:current-state (:game-start states)
     :a-rolls       0
     :b-rolls       0
     :states        states}
    (actions/apply-signal! {})
    (actions/apply-signal! {:dice-roll 6})
    (actions/apply-signal! {:dice-roll 5})
    (actions/apply-signal! {:dice-roll 7}))

(loop [state {:current-state (:game-start states)
              :a-rolls       0
              :b-rolls       0
              :states        states}]
  (cond (= (:current-state state) (:b-wins states)) (println "Game over: B wins!")
        (= (:current-state state) (:a-wins states)) (println "Game over: A wins!") 
        :else
    (let [roll (rand-int 10)] 
      (println "Current state: " (:name (:current-state state)))
      (println "A rolls: " (:a-rolls state))
      (println "B rolls: " (:b-rolls state))
      (println "Current roll: " roll)
      (println "")
      (recur (actions/apply-signal! state {:dice-roll roll})))))

(def matrix-vals
  (for [x (range 10)]
    (for [y (range 10)]
      {:x x :y y :tile nil})))
(def matrix (mapv vec matrix-vals))

(defn ->neighbor-coordinates
  [{:keys [x y]}]
  (let [neighbors (vec [{:x (dec x) :y (dec y)}
                        {:x (dec x) :y y}
                        {:x (dec x) :y (inc y)}
                        {:x x :y (dec y)}
                        {:x x :y (inc y)}
                        {:x (inc x) :y (dec y)}
                        {:x (inc x) :y y}
                        {:x (inc x) :y (inc y)}])]
    (filter (fn [{:keys [x y]}]
                (and (>= x 0) (>= y 0) (< x 10) (< y 10)))
            neighbors)))

(defn ->neighbors
  [coordinates matrix]
  (mapv (fn [{:keys [x y]}]
          (get-in matrix [x y])) (->neighbor-coordinates coordinates)))

(->neighbors {:x 9 :y 4} matrix)

(defn calculate-terrain
  [cell neighbors]
  (if (empty? neighbors)
    (assoc cell :tile (rand-nth [:water :grass :mountain]))
    (let [all-water?    (every? (fn [{:keys [tile]}] (= :water tile)) neighbors)
          all-mountain? (every? (fn [{:keys [tile]}] (= :mountain tile)) neighbors)
          all-grass?    (every? (fn [{:keys [tile]}] (= :grass tile)) neighbors)
          chosen-tile   (cond 
                          all-grass? (rand-nth [:water :grass :mountain])
                          all-water? (rand-nth [:water :grass])
                          all-mountain? (rand-nth [:mountian :grass])
                          (= (count (filter (fn [tile] (= tile :water)) neighbors)) 8) :water
                          (= (count (filter (fn [tile] (= tile :grass)) neighbors)) 8) :grass
                          :else :grass)]
      (assoc cell :tile chosen-tile))))

(defn calculate-terrain!
  [cell matrix]
  (let [neighbors           (->neighbors cell matrix)
        filled-in-neighbors (filter :tile neighbors)]
    (calculate-terrain cell filled-in-neighbors)))

(assoc-in matrix [0 1] (calculate-terrain! {:x 0 :y 1} matrix) )
(defn build-map!
  [matrix]
  (let [mtrx (atom matrix)]
    (doseq [x (range 10)
          y (range 10)]
      (let [cell (get-in @mtrx [x y])]
        (reset! mtrx (assoc-in @mtrx [x y] (calculate-terrain! cell @mtrx)))))
    @mtrx))

(build-map! matrix)
