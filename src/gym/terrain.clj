(ns gym.terrain)

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
                          :else :grass)]
      (assoc cell :tile chosen-tile))))

(defn calculate-terrain!
  [cell matrix]
  (let [neighbors           (->neighbors cell matrix)
        filled-in-neighbors (filter :tile neighbors)]
    (calculate-terrain cell filled-in-neighbors)))

(assoc-in matrix [0 1] (calculate-terrain! {:x 0 :y 1} matrix))
(defn build-map!
  [matrix]
  (let [mtrx (atom matrix)]
    (doseq [x (range 10)
            y (range 10)]
      (let [cell (get-in @mtrx [x y])]
        (reset! mtrx (assoc-in @mtrx [x y] (calculate-terrain! cell @mtrx)))))
    @mtrx))

(build-map! matrix)
