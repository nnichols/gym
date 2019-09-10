(ns gym.beer-xml.scrape
  (:require [clj-http.client :as http]
            [clojure.data.xml :as xml])) ;; XML is truly the worst

(def todo
  {1 "Download recipe listingh @ https://www.kaggle.com/jtrofe/beer-recipes"
   2 "Convert URL links to the form https://www.brewersfriend.com/homebrew/recipe/downloadbeerxml/5920"
   3 "Download beerXML and parse to brew-bot format"
   4 "Make brew-bot BeerXML compliant"
   5 "Update brew-bot to generate by styles using the above data"})

(defn kg->lb
  "BeerXMl is always in kg"
  [w]
  (* w 2.20462))

(defn kg->oz
  "BeerXMl is always in kg"
  [w]
  (* w 35.274))

(defn try-or-nil
  "Returns result of applying args to f inside a try/catch, returning nil in case of Exception."
  [f & args]
  (try
    (apply f args)
    (catch Exception e nil)))

(defn try-parse-double
  "Parses s to a double. Returns nil on failure."
  [s]
  (cond
    (nil? s)    nil
    (double? s) s
    (string? s) (try-or-nil #(Double/parseDouble %) s)))

(defn try-kg->lb
  [w]
  (let [weight (try-parse-double w)]
    (if weight
      (kg->lb weight)
      w)))

(defn try-kg->oz
  [w]
  (let [weight (try-parse-double w)]
    (if weight
      (kg->oz weight)
      w)))

(defn extract-tag
  [tag xml]
  (:content (first (filter #(= (:tag %) tag) xml))))

(defn value-at-tag
  [tag xml]
  (first (extract-tag tag (:content xml))))

(defn fermentables-xml->map
  [fermentables]
  (map #(hash-map :name   (value-at-tag :NAME %)
                  :weight (try-kg->lb (value-at-tag :AMOUNT %))) fermentables))

(defn hops-xml->map
  [hops]
  (map #(hash-map :name   (value-at-tag :NAME %)
                  :weight (try-kg->oz (value-at-tag :AMOUNT %))
                  :time   (value-at-tag :NAME %)) hops))

(defn yeasts-xml->map
  [yeasts]
  (map #(hash-map :name       (value-at-tag :NAME %)
                  :laboratory (value-at-tag :LABORATORY %)) yeasts))

(defn style-xml->map
  [style]
  (hash-map :name     (value-at-tag :NAME style)
            :category (value-at-tag :CATEGORY style)
            :type     (value-at-tag :TYPE style)))

(defn extras-xml->map
  [extras]
  (when extras
    (map #(hash-map :name   (value-at-tag :NAME %)
                    :use    (value-at-tag :USE %)
                    :time   (value-at-tag :TIME %)
                    :amount (try-kg->lb (value-at-tag :AMOUNT %))) extras)))

(defn fetch-recipe
  [url]
  (let [page (http/get url)]
    (when (= 200 (:status page))
      (-> page
          :body
          xml/parse-str
          :content
          first
          :content))))

(defn recipe->edn
  [recipe]
  (let [batch-size   (first (extract-tag :DISPLAY_BATCH_SIZE recipe))
        fermentables (fermentables-xml->map (extract-tag :FERMENTABLES recipe))
        hops         (hops-xml->map (extract-tag :HOPS recipe))
        yeasts       (yeasts-xml->map (extract-tag :YEASTS recipe))
        style        (style-xml->map (extract-tag :STYLE recipe))
        extras       (extras-xml->map (extract-tag :MISCS recipe))]
    {:batch-size   batch-size
     :fermentables fermentables
     :hops         hops
     :yeasts       yeasts
     :style        style
     :extras       extras}))

(defn try-me!
  []
  (let [recipe (fetch-recipe "https://www.brewersfriend.com/homebrew/recipe/beerxml1.0/29265")]
    (recipe->edn recipe)))
