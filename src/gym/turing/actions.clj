(ns gym.turing.actions)

(defn apply-state-transition!
  [{:keys [current-state states] :as state-machine} 
   {:keys [resulting-state action] :as _transition}
   signal]
  (let [new-state  (get states resulting-state)
        guarded? (and (:guard current-state) ((:guard current-state) state-machine signal))
        on-leave (:on-leave current-state)
        on-enter (:on-enter new-state)
        state (if (and (not guarded?) action)
                (action state-machine signal)
                state-machine)]
    (if guarded?
      (do (println "Guard failed! Returning unmodified state machine.")
          state-machine)
      (do (println (str "Transitioning from \"" (:name current-state) "\" to \"" (:name new-state)"\""))
          (when on-leave (on-leave state-machine signal))
          (when on-enter (on-enter state-machine signal))
          (assoc state :current-state new-state)))))

(defn apply-signal!
  [{:keys [current-state] :as state-machine} signal]
  (let [transitions       (:transitions current-state)
        valid-transitions (filter #((:match? %) current-state signal) transitions)
        transition        (first valid-transitions)]
    (cond
      (zero? (count valid-transitions)) (do (println "No legal state transitions. Returning unmodified state machine")
                                            state-machine)
      (= 1 (count valid-transitions))   (apply-state-transition! state-machine transition signal)
      :else                             (do (println "Ambiguous state transition! Multiple matching states found: [" (map :name valid-transitions "]"))
                                            (throw (IllegalStateException. "Ambiguous state transition!"))))))