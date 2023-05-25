(ns gym.beer.scrape
  (:require [again.core :as again]
            [clj-http.client :as http]
            [clj-xml.core :as xml]
            [clojure.data.xml :as c-xml]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [common-beer-format.util :as util]
            [common-beer-format.specs.fermentables :as fermentables]
            [common-beer-format.specs.hops :as hops]
            [common-beer-format.specs.primitives :as prim]
            [common-beer-format.specs.recipes :as recipes]
            [common-beer-format.specs.styles :as styles]
            [common-beer-format.specs.yeasts :as yeasts]
            [keg.core :as keg]
            [spec-tools.core :as st]
            [taoensso.timbre :as timbre]))

(def ^:const normalized-batch-size 19.0)

(defn ->recipe-url
  [recipe-number]
 (str "https://www.brewersfriend.com/homebrew/recipe/beerxml1.0/" recipe-number))

(defn fetch-recipe!
  [url]
  (timbre/infof "Fetching %s" url)
  (try
    (again/with-retries
      [1000 12000 24000]
      (let [page (http/get url)]
        (-> page 
            :body
            (str/replace #"\s\s+" "")
            c-xml/parse-str
            (xml/xml->edn {:remove-newlines? true})
            :recipes
            first
            :recipe)))
    (catch Exception e
      (timbre/error e "Failed to fetch!"))))

(keg/tap #'fetch-recipe! keg/pour-runtime-and-args)

(defn strip-extra-layers
  [ingredients ingredient-key]
  (map ingredient-key ingredients))

(defn coerce!
  [recipe]
  (let [normalized-recipe (st/coerce ::recipes/recipe recipe util/strict-transformer)]
    (-> normalized-recipe
        (select-keys [:name :boil-size :type :hops :style :boil-time :fermentables :yeasts])
        (assoc :id (java.util.UUID/randomUUID))
        (update :fermentables #(strip-extra-layers % :fermentable))
        (update :hops #(strip-extra-layers % :hop))
        (update :yeasts #(strip-extra-layers % :yeast)))))

(defn valid?
  [spec data]
  (if (spec/valid? spec data)
      true
    (do (timbre/error (spec/explain-str spec data))
        false)))

(defn validate!
  [{:keys [:name :boil-size :type :hops :style :boil-time :fermentables :yeasts]
    :as   r}]
  (let [valid-name?         (valid? ::prim/name name)
        valid-boil-size?    (valid? ::recipes/boil-size boil-size)
        valid-type?         (valid? ::recipes/type type)
        valid-hops?         (every? #(valid? ::hops/hop %) hops)
        valid-style?        (valid? ::styles/style style)
        valid-boil-time?    (valid? ::recipes/boil-time boil-time)
        valid-fermentables? (every? #(valid? ::fermentables/fermentable %) fermentables)
        valid-yeasts?       (every? #(valid? ::yeasts/yeast %) yeasts)]
    (if (and valid-name? valid-boil-size? valid-type? valid-hops? valid-style? valid-boil-time? valid-fermentables? valid-yeasts?)
      r
      (throw (ex-info "Failed to validate!" {:valid-name?         valid-name?
                                             :valid-boil-size?    valid-boil-size?
                                             :valid-type?         valid-type?
                                             :valid-hops          valid-hops?
                                             :valid-style?        valid-style?
                                             :valid-boil-time?    valid-boil-time?
                                             :valid-fermentables? valid-fermentables?
                                             :valid-yeasts?       valid-yeasts?
                                             :recipe              r})))))

(defn clean-name
  [n]
  (->> n
       str/lower-case
       str/trim
       (re-seq #"[a-z0-9]+")
       (str/join "-")))

(defn clean-ingredient
  [{:keys [amount] :as f} boil-size]
  (-> f
    (update :name clean-name)
    (assoc :amount (* (/ amount boil-size) normalized-batch-size)))) 

(defn normalize!
  [{:keys [fermentables yeasts hops boil-size]
    :as   r}]
  (let [normalized-fermentables    (mapv #(clean-ingredient % boil-size) fermentables)
        normalized-yeasts          (mapv #(clean-ingredient % boil-size) yeasts)
        normalized-hops            (mapv #(clean-ingredient % boil-size) hops)]
    (-> r
        (assoc :fermentables normalized-fermentables)
        (assoc :yeasts normalized-yeasts)
        (assoc :hops normalized-hops)
        (update :name clean-name))))

(defn fetch-and-normalize!
  [recipe-number]
  (let [offset         (rand-int 7500)
        sleep-schedule (+ 10000 offset)]
  (timbre/infof "Sleeping for %s" sleep-schedule)
  (Thread/sleep sleep-schedule)
  (try
    (-> recipe-number
        ->recipe-url
        fetch-recipe!
        coerce!
        validate!
        normalize!)
    (catch Throwable t
      (timbre/errorf "Error fetching recipe %s" recipe-number)))))
