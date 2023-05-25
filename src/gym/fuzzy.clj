(ns gym.fuzzy
  "Fuzzy string-matching utilities"
  (:require [clj-fuzzy.metrics :as fuzzy]
            [keg.core :as keg]))

(def ^:const match-threshold 0.75)
(def ^:const weights
  {:sorensen     0.15
   :tanimoto     0.25
   :jaro-match   0.2
   :jaro-winkler 0.25
   :lcs          0.15})

(defn sorensen-match
  [s1 s2]
  (fuzzy/sorensen s1 s2))

(defn tanimoto-match
  [s1 s2]
  (if (= "" s1 s2) ;; Comparing 2 blank strings throws an exception, but they are perfect matches
    1.0
    ;; In Tanimoto, 0 = 100% match. To normalize this, we need to invert scores
    (- 1.0 (fuzzy/tanimoto s1 s2))))

(defn jaro-match
  [s1 s2]
  (if (= "" s1 s2)
    1.0
    (fuzzy/jaro s1 s2)))

(defn jaro-winkler-match
  [s1 s2]
  (if (= "" s1 s2)
    1.0
    (fuzzy/jaro-winkler s1 s2)))

(defn longest-seq
  [xs ys]
  (if (> (count xs) (count ys)) xs ys))

(def longest-common-substring
  (memoize
   (fn [[x & xs] [y & ys]]
     (cond
       (or (nil? x)
           (nil? y)) nil
       (= x y)       (cons x (longest-common-substring xs ys))
       :else         (longest-seq (longest-common-substring (cons x xs) ys)
                                  (longest-common-substring xs (cons y ys)))))))

(defn longest-common-substring-match
  [s1 s2]
  (if (= "" s1 s2) ;; Comparing 2 blank strings throws an exception, but they are perfect matches
    1.0
    (let [longest-length (count (longest-seq s1 s2))
          lcs-length     (count (longest-common-substring s1 s2))]
      (/ (* lcs-length 1.0) longest-length))))


(defn score-string-match
  "Compare `s1` and `s2` with multiple distance metrics"
  [s1 s2]
  {:sorensen     (sorensen-match s1 s2)
   :tanimoto     (tanimoto-match s1 s2)
   :jaro-match   (jaro-match s1 s2)
   :jaro-winkler (jaro-winkler-match s1 s2)
   :lcs          (longest-common-substring-match s1 s2)})

(keg/tap #'score-string-match keg/pour-runtime-args-and-return)

(defn weighted-match-average
  "Add up the fuzzy-match scores returned by `score-string-match`, and return an average.
   This is currently a fair average, but weights may be applied as various algorithms prove more/less useful."
  [{:keys [sorensen tanimoto jaro-match jaro-winkler lcs]}]
  (let [adj-scores {:sorensen     (* (:sorensen weights) sorensen)
                    :tanimoto     (* (:tanimoto weights) tanimoto)
                    :jaro-match   (* (:jaro-match weights) jaro-match)
                    :jaro-winkler (* (:jaro-winkler weights) jaro-winkler)
                    :lcs          (* (:lcs weights) lcs)}]
    (reduce + (vals adj-scores))))
