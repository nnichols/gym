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
         (map #(hash-map (keywordize (value-at-tag :NAME %)) 1) yeasts)))

(defn extras-xml->map
  [extras gallons]
  (when extras
    (apply merge (map #(hash-map (keywordize (value-at-tag :NAME %))
                                 (or (try-normalized-kg->lb (value-at-tag :AMOUNT %) gallons) 1)) extras))))

(defn fetch-recipe
  [recipe-number]
  (let [url (str "https://www.brewersfriend.com/homebrew/recipe/beerxml1.0/" recipe-number)
        page (http/get url)]
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
  [recipe-number]
  (when-let [recipe (fetch-recipe recipe-number)]
    (do
      (Thread/sleep 15000)
      (println recipe-number)
      (try-or-nil #(recipe-xml->edn %) recipe))))

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
  (aggregate-recipes (map fetch-convert-normalize! [364091
                                                    239546
                                                    301983
                                                    294457
                                                    204213
                                                    297745
                                                    226659
                                                    300859
                                                    199130
                                                    381134
                                                    387775
                                                    296043
                                                    310884
                                                    259963
                                                    301304
                                                    255951
                                                    367482
                                                    309153
                                                    377016
                                                    233282
                                                    314037
                                                    304920
                                                    420474
                                                    442391
                                                    294656
                                                    437319
                                                    332974
                                                    435648
                                                    458422
                                                    376914
                                                    387502
                                                    422361
                                                    332798
                                                    325419
                                                    303405
                                                    355250
                                                    333552
                                                    323710
                                                    262301
                                                    343476
                                                    390906
                                                    378266
                                                    347870
                                                    402307
                                                    428667
                                                    395763
                                                    405338
                                                    446919
                                                    400512
                                                    387747
                                                    339535
                                                    394921
                                                    452416
                                                    425909
                                                    354782
                                                    377348
                                                    342117
                                                    347057
                                                    388086
                                                    484633
                                                    329135
                                                    341314
                                                    314979
                                                    404852
                                                    382191
                                                    431261
                                                    359472
                                                    365195
                                                    373674
                                                    347951
                                                    324695
                                                    438540
                                                    385967
                                                    367096
                                                    444260
                                                    386219
                                                    391235
                                                    300535
                                                    319321
                                                    396997
                                                    354344
                                                    372537
                                                    305167
                                                    389648
                                                    440814
                                                    402947
                                                    507156
                                                    379196
                                                    308312
                                                    372224
                                                    376723
                                                    346282
                                                    338704
                                                    389543
                                                    317654
                                                    466642
                                                    384529
                                                    498520
                                                    452803
                                                    336435
                                                    393971
                                                    373606
                                                    304798
                                                    346547
                                                    372743
                                                    408989
                                                    356619
                                                    407999
                                                    390132
                                                    433856
                                                    311634
                                                    310402
                                                    325714
                                                    399717
                                                    332412
                                                    318471
                                                    380263
                                                    382732
                                                    385398
                                                    463042
                                                    445203
                                                    335462
                                                    337845
                                                    385429
                                                    390058
                                                    460538
                                                    519872
                                                    372165
                                                    383225
                                                    464850
                                                    440308
                                                    407036
                                                    407055
                                                    371108
                                                    394214
                                                    464971
                                                    406083
                                                    495176
                                                    382489
                                                    316882
                                                    400516
                                                    344088
                                                    519744
                                                    500071
                                                    404039
                                                    516343
                                                    486223
                                                    501076
                                                    393634
                                                    392179
                                                    388537
                                                    494937
                                                    445057
                                                    396941
                                                    307955
                                                    497174
                                                    413416
                                                    397702
                                                    394616
                                                    510488
                                                    390478
                                                    401206
                                                    408254
                                                    463487
                                                    330964
                                                    384446
                                                    551109
                                                    394442
                                                    377957
                                                    502393
                                                    464293
                                                    474109
                                                    510041
                                                    397378
                                                    536772
                                                    371602
                                                    305505
                                                    469678
                                                    384074
                                                    391553
                                                    380055
                                                    520276
                                                    502721
                                                    450429
                                                    492054
                                                    479799
                                                    545656
                                                    499594
                                                    491991
                                                    524892
                                                    483902
                                                    536551
                                                    462927
                                                    526046
                                                    544353
                                                    531322
                                                    473393
                                                    529061
                                                    395884
                                                    514922
                                                    489733
                                                    501780
                                                    521074
                                                    564167
                                                    524751
                                                    454789
                                                    516859
                                                    480756
                                                    193892
                                                    538623
                                                    535327
                                                    517800
                                                    525528
                                                    536923
                                                    455160
                                                    539197
                                                    535281
                                                    473864
                                                    485020
                                                    521368
                                                    536249
                                                    524276
                                                    518306
                                                    530578
                                                    514546
                                                    528802
                                                    508164
                                                    529788
                                                    513256
                                                    534536
                                                    528053
                                                    512229
                                                    554587
                                                    525156
                                                    508794
                                                    535710
                                                    542763
                                                    534394
                                                    543752
                                                    538647
                                                    516926
                                                    523473
                                                    521244
                                                    535135
                                                    528480
                                                    534640
                                                    510613
                                                    536124
                                                    516873
                                                    562602
                                                    522945
                                                    533479
                                                    516642
                                                    515020
                                                    475090
                                                    532760
                                                    529295
                                                    543931
                                                    556229
                                                    525061
                                                    528508
                                                    532991
                                                    533768
                                                    535196
                                                    559618
                                                    530454
                                                    533802
                                                    551556
                                                    520920
                                                    551470
                                                    528233
                                                    569545
                                                    534638
                                                    519288
                                                    529807
                                                    550304
                                                    548676
                                                    532967
                                                    519093
                                                    543990
                                                    526610
                                                    532300
                                                    413055
                                                    527606
                                                    482369
                                                    556918
                                                    553410
                                                    523055
                                                    562302
                                                    560797
                                                    547368
                                                    547750
                                                    510952
                                                    559026
                                                    580783
                                                    569609
                                                    584807
                                                    566456
                                                    553484
                                                    474164
                                                    379006
                                                    432366
                                                    533141
                                                    566601
                                                    593162
                                                    448839
                                                    587124
                                                    592320
                                                    581178
                                                    532066
                                                    533513
                                                    591242
                                                    579238
                                                    605643
                                                    436135
                                                    592943
                                                    601013
                                                    581255
                                                    577874
                                                    598772
                                                    604150
                                                    596235
                                                    607312
                                                    608849
                                                    511509
                                                    610961
                                                    618449
                                                    620251
                                                    618757
                                                    613947
                                                    618411
                                                    618960
                                                    620734])))
