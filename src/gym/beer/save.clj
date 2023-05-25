(ns gym.beer.save
  (:require [cheshire.core :as json]
            [cheshire.generate :as generate]
            [honeysql.core :as sql]
            [honeysql.helpers :as helpers]
            [honeysql-postgres.helpers :as psqlh]
            [gym.beer.analysis :as analysis]
            [keg.core :as keg]
            [next.jdbc.date-time]
            [next.jdbc.prepare :as prepare]
            [next.jdbc.result-set :as rs]
            [next.jdbc :as jdbc]
            [taoensso.timbre :as timbre])
  (:import (java.sql Array PreparedStatement Timestamp)
           (org.postgresql.util PGobject)))

(def as-maps
  "Return maps representing db rows instead of namespace qualified maps"
  {:builder-fn rs/as-unqualified-lower-maps})

(defn clojure->pgobject
  "Transforms Clojure data to a PGobject that contains the data as JSON.
   PGObject type defaults to `jsonb` but can be changed via metadata key `:pgtype`"
  [x]
  (when x
    (let [pgtype (or (:pgtype (meta x)) "jsonb")]
      (doto (PGobject.)
        (.setType pgtype)
        (.setValue (json/generate-string x))))))

(defn pgobject->clojure
  "Transform PGobject containing `json` or `jsonb` value to Clojure data."
  [^org.postgresql.util.PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (when value
        (with-meta (json/parse-string value true) {:pgtype type}))
      value)))

(set! *warn-on-reflection* true)

(extend-protocol generate/JSONable
  java.time.Instant
  (to-json  [dt gen]
    (generate/write-string gen (str dt))))

;; When loading a SQL paramamter that's a clojure data structure, transform it to a JSON PGObject
(extend-protocol prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [clojure-map ^PreparedStatement sql-statement impl]
    (.setObject sql-statement impl (clojure->pgobject clojure-map)))

  clojure.lang.IPersistentVector
  (set-parameter [clojure-vector ^PreparedStatement sql-statement impl]
    (.setObject sql-statement impl (clojure->pgobject clojure-vector))))

;; When loading a PGObject, convert it to a Clojure data clojure data structure
(extend-protocol rs/ReadableColumn
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject value _column-label]
    (pgobject->clojure value))
  (read-column-by-index [^org.postgresql.util.PGobject value _result-set-meta _index]
    (pgobject->clojure value))

  Array
  (read-column-by-label [^Array v _column-label]
    (vec (.getArray v)))
  (read-column-by-index [^Array v _result-set-meta _index]
    (vec (.getArray v)))

  Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]     (.toInstant v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3] (.toInstant v)))

(defn build-datasource!
  []
  (jdbc/get-datasource {:user     (System/getenv "DB_USER")
                        :password (System/getenv "DB_PASS")
                        :dbtype   "postgres"
                        :dbname   "beer"}))

(defn insert-recipe-query
  [r]
  (let [fermentables (clojure->pgobject (:fermentables r))
        yeasts       (clojure->pgobject (:yeasts r))
        hops         (clojure->pgobject (:hops r))
        style        (:category (:style r))
        recipe-name  (:name r)
        id           (:id r)
        row          [{:id           id
                       :name         recipe-name
                       :style        style
                       :fermentables fermentables
                       :yeast        yeasts
                       :hops         hops}]]
    (sql/format (-> (helpers/insert-into :recipes)
                    (helpers/values row)))))

(keg/tap #'insert-recipe-query keg/pour-runtime)

(defn ->ingredient-row
  [ingredient]
  (let [name   (:name ingredient)
        i-type (or (:type ingredient) (:use ingredient))
        body   (clojure->pgobject ingredient)]
    {:id   (java.util.UUID/randomUUID)
     :name name
     :type i-type
     :body body}))

(defn insert-ingredient-query
  [ingredients ingredient-type]
  (let [rows (mapv ->ingredient-row ingredients)]
    (sql/format (-> (helpers/insert-into ingredient-type)
                    (helpers/values rows)))))

(keg/tap #'insert-ingredient-query keg/pour-runtime)

(defn save-recipe!
  [r]
  (timbre/infof "Persisting %s to Postgres" (:name r))
  (try
    (let [recipe-query       (insert-recipe-query r)
          fermentables-query (insert-ingredient-query (:fermentables r) :fermentables)
          hops-query         (insert-ingredient-query (:hops r) :hops)
          yeasts-query       (insert-ingredient-query (:yeasts r) :yeasts)]
      (with-open [conn (jdbc/get-connection (build-datasource!))]
        (jdbc/execute! conn recipe-query as-maps)
        (jdbc/execute! conn fermentables-query as-maps)
        (jdbc/execute! conn hops-query as-maps)
        (jdbc/execute! conn yeasts-query as-maps)))
    (catch Throwable t
      (timbre/errorf t "Error persisting %s to Postgres" (:name r)))))

(keg/tap #'save-recipe! keg/pour-runtime)

;; f h y s
;; f->f
;; f->h
;; f->y
;; f->s
;; h->h
;; h->y
;; h->s
;; y->s

(defn ->relationship
  [e1 e1-type e2 e2-type]
  {:id              (java.util.UUID/randomUUID)
   :entity_one_name (name e1)
   :entity_one_type (name e1-type)
   :entity_two_name (name e2)
   :entity_two_type (name e2-type)
   :occurences      1})

(defn fermentable->relationships
  [fermentable fermentables hops yeasts style]
  (let [f-key              (first (keys fermentable))
        other-fermentables (dissoc fermentables f-key)
        ->relation         (partial ->relationship f-key :fermentable)
        f-relations        (map #(->relation (first %) :fermentable) other-fermentables)
        h-relations        (map #(->relation (first %) :hop) hops)
        y-relations        (map #(->relation (first %) :yeast) yeasts)
        s-relations        [(->relation style :style)]]
    (into [] (concat f-relations h-relations y-relations s-relations))))

(defn fermentable-relationships
  [fermentables hops yeasts style]
  (letfn [(reducing-fn
            [acc k v]
            (concat acc (fermentable->relationships {k v} fermentables hops yeasts style)))]
    (reduce-kv reducing-fn [] fermentables)))

(defn hop->relationships
  [hop hops yeasts style]
  (let [h-key       (first (keys hop))
        other-hops  (dissoc hops h-key)
        ->relation  (partial ->relationship h-key :hop)
        h-relations (map #(->relation (first %) :hop) other-hops)
        y-relations (map #(->relation (first %) :yeast) yeasts)
        s-relations [(->relation style :style)]]
    (into [] (concat h-relations y-relations s-relations))))

(defn hop-relationships
  [hops yeasts style]
  (letfn [(reducing-fn
            [acc k v]
            (concat acc (hop->relationships {k v} hops yeasts style)))]
    (reduce-kv reducing-fn [] hops)))

(defn yeast->relationships
  [yeast yeasts style]
  (let [y-key        (first (keys yeast))
        other-yeasts (dissoc yeasts y-key)
        ->relation   (partial ->relationship y-key :yeast)
        y-relations  (map #(->relation (first %) :yeast) other-yeasts)
        s-relations  [(->relation style :style)]]
    (into [] (concat y-relations s-relations))))

(defn yeast-relationships
  [yeasts style]
  (letfn [(reducing-fn
            [acc k v]
            (concat acc (yeast->relationships {k v} yeasts style)))]
    (reduce-kv reducing-fn [] yeasts)))

(defn insert-relationships-query
  [relationships]
  (-> (helpers/insert-into :relationship)
      (helpers/values relationships)
      (psqlh/upsert (-> (psqlh/on-conflict :entity_one_name :entity_one_type :entity_two_name :entity_two_type)
                        (psqlh/do-update-set! [:occurences (sql/raw "relationship.occurences + 1::INTEGER")])))
      (sql/format {:pretty true}))

(defn save-analysis!
  [{:keys [fermentables hops yeasts]
    :as   _weights} style]
  (let [relationship-rows (into [] 
                                (concat 
                                 (fermentable-relationships fermentables hops yeasts style)
                                 (hop-relationships hops yeasts style)
                                 (yeast-relationships yeasts style)))]
    (println relationship-rows)
    (with-open [conn (jdbc/get-connection (build-datasource!))]
      (jdbc/execute! conn (insert-relationships-query relationship-rows) as-maps))))

(comment 
  (def recipe
    (with-open [conn (jdbc/get-connection (build-datasource!))]
      (first (jdbc/execute! conn ["SELECT * FROM RECIPES ORDER BY name LIMIT 1"] as-maps))))
  (save-analysis! (analysis/recipe-analysis recipe) :ipa)

  (with-open [conn (jdbc/get-connection (build-datasource!))]
    (let [recipe (jdbc/execute! conn ["SELECT * FROM RECIPES ORDER BY name LIMIT 1"] as-maps)
        ;  unique-hop         (->name (jdbc/execute! conn ["SELECT DISTINCT(name) FROM HOPS ORDER BY name"] as-maps))
        ;  unique-yeast       (->name (jdbc/execute! conn ["SELECT DISTINCT(name) FROM YEASTS ORDER BY name"] as-maps))
          ]
      (timbre/info recipe)
      ;(mapv #(analysis/matching-ingredient % analysis/all-fermentables analysis/fermentable-names) unique-fermentable)
      ;(timbre/infof "Unique hops: %s" unique-hop)
      ;(timbre/infof "Unique yeasts: %s" unique-yeast)
      ))
  
  )
  
  