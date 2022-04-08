(ns gym.turing)

(defn initialize?
  [_state _signal]
  true)

(defn win?
  [_state {:keys [dice-roll] :as _signal}]
  (= 7 dice-roll))

(defn re-roll?
  [_state {:keys [dice-roll] :as _signal}]
  (= 6 dice-roll))

(defn lose?
  [_state {:keys [dice-roll] :as _signal}]
  (not (or (= 6 dice-roll) (= 7 dice-roll))))

(def states
  {:game-start {:name "Game started!"
                :initial?    true
                :transitions [{:name            :->a
                               :match?          #'initialize?
                               :resulting-state :a}]}
   :a          {:name "Player A's turn"
                :transitions [{:name            :->a-wins
                               :match?          #'win?
                               :resulting-state :a-wins}
                              {:name            :a-reroll
                               :match?           #'re-roll?
                               :resulting-state :a}
                              {:name            :->b
                               :match?          #'lose?
                               :resulting-state :b}]}
   :b          {:name "Player B's turn"
                :transitions [{:name            :->b-wins
                               :match?          #'win?
                               :resulting-state :b-wins}
                              {:name            :b-reroll
                               :match?           #'re-roll?
                               :resulting-state :b}
                              {:name            :->a
                               :match?          #'lose?
                               :resulting-state :a}]}
   :a-wins     {:name      "Player A wins"
                :terminal? true}
   :b-wins     {:name      "Player B wins"
                :terminal? true}})

(defn state-transition
  [{:keys [transitions] :as state} signal]
  (let [valid-transitions (filter #((:match? %) state signal) transitions)]
    (cond
      (zero? (count valid-transitions)) (do (println "No legal state transitions. Returning current state")
                                            state)
      (= 1 (count valid-transitions))   (do (println "Transitioning from" (:name state) "to" (:name (first valid-transitions)))
                                            (get states (:resulting-state (first valid-transitions))))
      :else                             (do (println "Ambiguous state transition! Multiple matching states found: " (map :name valid-transitions))
                                            (throw (IllegalStateException. "Ambiguous state transition!"))))))

(def initial-state
  (let [initial-states (filter :initial? (vals states))]
    (if (= 1 (count initial-states))
      (first initial-states)
      (throw (IllegalStateException. "There must be exactly one initial state!")))))

(def terminal-states
  (filter :terminal? (vals states)))

(defn play-game
  []
  (loop [game-state initial-state
         turn       0]
    (if (:terminal? game-state)
      (do (println (:name game-state) "after" turn "turns!")
          turn)
      (recur (state-transition game-state {:dice-roll (rand-int 10)}) (inc turn)))))

(play-game)
