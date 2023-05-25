(ns gym.beer.analysis
  (:require [clojure.string :as str]
            [common-beer-format.data.data :as source]
            [gym.fuzzy :as fuzzy]
            [taoensso.timbre :as timbre]))

(def all-fermentables source/all-fermentables)
(def all-hops source/all-hops)
(def all-yeasts source/all-yeasts)
(def all-style-guides source/all-style-guides)

(def fermentable-names
  (->> all-fermentables
       keys
       (map name)
       set))

(def hop-names
  (->> all-hops
       keys
       (map name)
       set))

(def yeast-names
  (->> all-yeasts
       keys
       (map name)
       set))

(def style-names
  (->> all-style-guides
       vals
       (map :category)
       set))

(defn best-match
  [ingredient-name ingredient-set]
  (let [reducing-fn (fn [acc i-name]
                      (let [scores    (fuzzy/score-string-match ingredient-name i-name)
                            avg-score (fuzzy/weighted-match-average scores)]
                        (if (>= avg-score fuzzy/match-threshold)
                          (conj acc {:name       (keyword i-name)
                                     :confidence avg-score})
                          acc)))
        matches     (reduce reducing-fn [] ingredient-set)]
    (if (empty? matches)
      ::empty
      (first (sort-by :confidence > matches)))))

(defn matching-ingredient
  [ingredient ingredient-map ingredient-names]
  (let [ingredient-name   (:name ingredient)
        exact-name-match? (contains? ingredient-names ingredient-name)]
    (if exact-name-match?
      (do (timbre/infof "Exact match found for %s" ingredient-name)
          (get ingredient-map (keyword ingredient-name)))
      (let [closest-match (best-match ingredient-name ingredient-names)]
        (if (= ::empty closest-match)
          (do (timbre/errorf "No matching ingredient found for %s" ingredient-name)
              nil)
          (do (timbre/infof "Stochastic match found for %s - %s with confidence %s" ingredient-name (:name closest-match) (:confidence closest-match))
              (get ingredient-map (keyword (:name closest-match)))))))))

(defn matching-fermentable
  "Prefer canonical information from common-beer-format to scraped data for non-recipe-centric stats"
  [{:keys [amount add-after-boil] :as fermentable}]
  (if-let [matching (matching-ingredient fermentable source/all-fermentables fermentable-names)]
    (assoc matching :amount amount :add-after-boil add-after-boil)
    fermentable))

(defn matching-hop
  "Prefer canonical information from common-beer-format to scraped data for non-recipe-centric stats"
  [hop]
  (if-let [matching (matching-ingredient hop source/all-hops hop-names)]
    (merge matching (select-keys hop [:amount :use :time :form]))
    hop))

(defn matching-yeast
  "Prefer canonical information from common-beer-format to scraped data for non-recipe-centric stats"
  [{:keys [amount] :as yeast}]
  (if-let [matching (matching-ingredient yeast source/all-yeasts yeast-names)]
    (assoc matching :amount amount)
    yeast))

(defn ->clean-name
  [n]
  (->> n
       :name
       str/lower-case
       str/trim
       (re-seq #"[a-z0-9]+")
       (str/join "-")
       keyword))

(defn recipe->weights
  [{:keys [hops fermentables yeast]}]
  (letfn [(->weights [acc f] (assoc acc (->clean-name f) (:amount f)))]
    {:fermentables (reduce ->weights {} fermentables) 
     :hops         (reduce ->weights {} hops) 
     :yeasts       (reduce ->weights {} yeast)}))

(def style-mapping
  {"India Pale Ale (IPA)"          "IPA"
   "Stout"                         "American Porter And Stout"
   "Belgian and French Ale"        "Belgian Ale"
   "Light Hybrid Beer"             "Specialty Beer"
   "Porter"                        "American Porter And Stout"
   "English Pale Ale"              "British Bitter"
   "Amber Hybrid Beer"             "Amber Bitter European Lager"
   "English Brown Ale"             "Amber And Brown American Beer"
   "NO PROFILE SELECTED ------"    "Historical Beer"
   "Spice/Herb/Vegetable Beer"     "Amber Bitter European Beer"
   "Sour Ale"                      "European Sour Ale"
   "Mead"                          "Historical Beer"
   "Smoke Flavored/Wood-Aged Beer" "Historical Beer"
   "Pilsner"                       "Czech Lager"
   "European Amber Lager"          "Amber Malty European Lager"
   "Cider or Perry"                "Historical Beer"
   "Bock"                          "German Wheat Beer"
   "Light Lager"                   "International Lager"
   "Dark Lager"                    "Dark European Lager"})

(defn matching-style
  [style]
  (let [clean-style (str/trim style)
        style (or (get style-mapping clean-style) clean-style)
        exact-name-match? (contains? style-names style)]
    (if exact-name-match?
      (do (timbre/infof "Exact match found for \"%s\"" style)
          style)
      (let [closest-match (best-match style style-names)]
        (if (= ::empty closest-match)
          (do (timbre/errorf "No matching style found for \"%s\"" style)
              nil)
          (do (timbre/infof "Stochastic match found for \"%s\" - \"%s\" with confidence %s" style (:name closest-match) (:confidence closest-match))
              (:name closest-match)))))))

(defn recipe-analysis
  [recipe]
  (let [style (matching-style (:style recipe))]
    {:weights (recipe->weights recipe)
     :style style}))
