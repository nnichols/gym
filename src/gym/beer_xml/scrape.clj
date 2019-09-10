(ns gym.beer-xml.scrape
  (:require [clj-http.client :as http]
            [clojure.data.xml :as xml]))

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

(defn fermentables-xml->map
  [fermentables]
  (map #(hash-map :name   (first (extract-tag :NAME (:content %))) ;; XML is the worst
                  :weight (try-kg->lb (first (extract-tag :AMOUNT (:content %))))) fermentables))

(defn hops-xml->map
  [hops]
  (map #(hash-map :name   (first (extract-tag :NAME (:content %)))
                  :weight (try-kg->oz (first (extract-tag :AMOUNT (:content %))))
                  :time   (first (extract-tag :NAME (:content %)))) hops))

(defn yeasts-xml->map
  [yeasts]
  (map #(hash-map :name       (first (extract-tag :NAME (:content %)))
                  :laboratory (first (extract-tag :LABORATORY (:content %)))) yeasts))

(defn style-xml->map
  [style]
  (hash-map :name     (first (extract-tag :NAME style))
            :category (first (extract-tag :CATEGORY style))
            :type     (first (extract-tag :TYPE style))))

(defn extras-xml->map
  [extras]
  (when extras
    (map #(hash-map :name   (first (extract-tag :NAME (:content %)))
                    :use    (first (extract-tag :USE (:content %)))
                    :time   (first (extract-tag :TIME (:content %)))
                    :amount (try-kg->lb (first (extract-tag :AMOUNT (:content %))))) extras)))

(defn try-me!
  []
  (let [recipe (-> (http/get "https://www.brewersfriend.com/homebrew/recipe/beerxml1.0/29265")
                   :body
                   xml/parse-str
                   :content
                   first
                   :content)
        batch-size   (first (extract-tag :DISPLAY_BATCH_SIZE recipe))
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
