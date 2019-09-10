(ns gym.beer-xml.scrape
  (:require [clj-http.client :as http]
            [clojure.data.xml :as xml]
            [clojure.string :as cs])) ;; XML is truly the worst

(def todo
  {1 "Download recipe listingh @ https://www.kaggle.com/jtrofe/beer-recipes"
   2 "Convert URL links to the form https://www.brewersfriend.com/homebrew/recipe/downloadbeerxml/5920"
   3 "Download beerXML and parse to brew-bot format"
   4 "Make brew-bot BeerXML compliant"
   5 "Update brew-bot to generate by styles using the above data"})

(defn keywordize
  [s]
  (keyword (cs/join "-" (re-seq #"[a-zA-Z0-9]+" (cs/lower-case (str s))))))

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

(defn kg->lb
  "BeerXMl is always in kg"
  [w]
  (* w 2.20462))

(defn kg->oz
  "BeerXMl is always in kg"
  [w]
  (* w 35.274))

(defn l->gal
  "BeerXMl is always in l"
  [l]
  (* l 0.264172))

(defn try-normalized-kg->lb
  [w gallons]
  (when-let [weight (try-parse-double w)]
    (/ (kg->lb weight) gallons)))

(defn try-normalized-kg->oz
  [w gallons]
  (when-let [weight (try-parse-double w)]
    (/ (kg->oz weight) gallons)))

(defn try-l->gal
  [l]
  (let [volume (try-parse-double l)]
    (if volume
      (l->gal volume)
      l)))

(defn extract-tag
  [tag xml]
  (:content (first (filter #(= (:tag %) tag) xml))))

(defn value-at-tag
  [tag xml]
  (first (extract-tag tag (:content xml))))

(defn fermentables-xml->map
  [fermentables gallons]
  (apply merge
         (map #(hash-map (keywordize (value-at-tag :NAME %))
                         (try-normalized-kg->lb (value-at-tag :AMOUNT %) gallons)) fermentables)))

(defn hops-xml->map
  [hops gallons]
  (apply merge
    (map #(hash-map (keywordize (value-at-tag :NAME %))
                               (try-normalized-kg->oz (value-at-tag :AMOUNT %) gallons)) hops)))

(defn yeasts-xml->map
  [yeasts]
  (apply merge
    (map #(hash-map (keywordize (value-at-tag :NAME %))
                  1) yeasts)))

(defn extras-xml->map
  [extras gallons]
  (when extras
    (apply merge (map #(hash-map (keywordize (value-at-tag :NAME %))
                    (or (try-normalized-kg->lb (value-at-tag :AMOUNT %) gallons) 1)) extras))))

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

(defn recipe-xml->edn
  [recipe]
  (let [boil-size    (try-l->gal (first (extract-tag :BOIL_SIZE recipe)))
        fermentables (fermentables-xml->map (extract-tag :FERMENTABLES recipe) boil-size)
        hops         (hops-xml->map (extract-tag :HOPS recipe) boil-size)
        extras       (extras-xml->map (extract-tag :MISCS recipe) boil-size)
        yeasts       (yeasts-xml->map (extract-tag :YEASTS recipe))]
    {:boil-size    boil-size
     :fermentables fermentables
     :hops         hops
     :yeasts       yeasts
     :extras       extras}))

(defn fetch-convert-normalize!
  [url]
  (when-let [recipe (fetch-recipe url)]
    (try-or-nil #(recipe-xml->edn %) recipe)))

(defn aggregate-recipes
  [recipe-list]
  (let [acc-map {:fermentables {} :hops {} :yeasts {} :extras {}}
        acc-fn (fn [acc next]
                 (if next
                   (hash-map :fermentables (merge-with + (:fermentables acc) (:fermentables next))
                              :hops         (merge-with + (:hops acc) (:hops next))
                              :yeasts       (merge-with + (:yeasts acc) (:yeasts next))
                              :extras       (merge-with + (:extras acc) (:extras next)))
                    acc))]
    (reduce acc-fn acc-map recipe-list)))

(defn try-me!
  []
  (aggregate-recipes [(fetch-convert-normalize! "https://www.brewersfriend.com/homebrew/recipe/beerxml1.0/29265")
                      (fetch-convert-normalize! "https://www.brewersfriend.com/homebrew/recipe/beerxml1.0/401540")]))
