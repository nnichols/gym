(ns gym.benchmarking.malli
  (:require [criterium.core :as crit]
            [malli.core :as m]
            [malli.transform :as mt]))

(def system-characteristics
  "The OS, JVM, and CPU settings that may impact performance.
   
   Last executed on:
   {:jdk.debug release
    :spec-vendor Oracle Corporation
    :java.vm.name OpenJDK 64-Bit Server VM
    :spec-name Java Virtual Machine Specification
    :java.vm.version 23.0.1
    :vm-version 23.0.1
    :clojure.debug false
    :sun.java.launcher SUN_STANDARD
    :java.vendor Homebrew
    :os.version 14.7.1
    :sun.management.compiler HotSpot 64-Bit Tiered Compilers
    :name 30499@D74JR9LcholsMac.home
    :clojure-version-string 1.12.0
    :user.language en
    :java.vm.info mixed mode, emulated-client, sharing
    :java.class.version 67.0
    :java-runtime-version 23.0.1
    :java-version 23.0.1
    :vm-name OpenJDK 64-Bit Server VM
    :java.runtime.version 23.0.1
    :vm-vendor Homebrew
    :clojure-version {:major 1
    :minor 12
    :incremental 0
    :qualifier nil}
    :spec-version 23
    :os.arch aarch64
    :sun-arch-data-model 64
    :input-arguments [-Dfile.encoding=UTF-8, -XX:-OmitStackTraceInFastThrow, -XX:+TieredCompilation, -XX:TieredStopAtLevel=1]
    :java.vm.specification.vendor Oracle Corporation
    :os.name Mac OS X
    :sun.cpu.endian little}"
  (merge (crit/runtime-details)
         (reduce-kv (fn [m k v]
                      (assoc m (keyword k) v))
                    {}
                    (select-keys (into {} (crit/system-properties))
                                 ["clojure.debug"
                                  "file-encoding"
                                  "java.class.version"
                                  "java.runtime.version"
                                  "java.vendor"
                                  "java.vm.info"
                                  "java.vm.name"
                                  "java.vm.specification.vendor"
                                  "java.vm.version"
                                  "jdk.debug"
                                  "os.arch"
                                  "os.name"
                                  "os.version"
                                  "sun.cpu.endian"
                                  "sun.java.launcher"
                                  "sun.management.compiler"
                                  "user.language"]))))

(defn print-breaking-line
  "Print a line of dashes to break up output"
  []
  (println (apply str (take 80 (repeat "-")))))

(defn print-separating-line
  "Print a line of stars to break up output"
  []
  (println (apply str (take 80 (repeat "*")))))

(def sample-settings
  "The settings to be used across all benchmarking."
  {:samples 500})

(def sample-transformer
  "A complex malli transformer composed of multiple steps."
  (mt/transformer
   mt/string-transformer
   mt/strip-extra-keys-transformer
   mt/collection-transformer))

(def schema
  "A complex malli schema that uses multiple out-of-the-box tools"
  [:map
   [:any-key :any]
   [:nil-key :nil]
   [:uuid-key :uuid]
   [:string-key :string]
   [:int-key :int]
   [:double-key :double]
   [:boolean-key :boolean]
   [:keyword-key :keyword]
   [:big-key [:and :int [:> 10]]]
   [:tags [:set keyword?]]
   [:map-key [:map 
              [:a :int]
              [:b :int]]]])

(def ^:const inlining-lookup
  "An in-linable malli schema"
  schema)

(def schema-as-schema
  "A pre-computed malli schema"
  (m/schema schema))

(def schema-validator
  "A pre-computed malli validator"
  (m/validator schema-as-schema))

(def schema-decoder
  "A pre-computed malli decoder"
  (m/decoder schema-as-schema sample-transformer))

(def sample-data
  "Data which conforms to `schema`"
  {:tags        #{:XrLEzYD/_*58w.
                  :-?8++*wa/!Q-
                  :n/Y615S68
                  :m../*4gq
                  :X_8?T9/*
                  :L57?m/pY!
                  :HL_:W1y/h-6M
                  :yz+*-vb/xtKorS6
                  :Q*/cfG+
                  :!/!J79!
                  :N_/gJH8:43
                  :.hY/xZ*q
                  :DD.:!w-6/Ba?Z?
                  :Y-/zg9Jra-x},
   :nil-key     nil,
   :string-key  "SU7c19Vpf",
   :map-key     {:a -2
                 :b -12951},
   :big-key     3253659,
   :boolean-key false,
   :int-key     -1,
   :double-key  0.00823211669921875,
   :keyword-key :-.*+6+s,
   :uuid-key    #uuid "7e9485d0-29cd-422f-adb2-2466bf039b98",
   :any-key     nil})

(def sample-unparsed-data
  "Data which decodes into a shape supported by `schema`"
  {:tags        [:XrLEzYD/_*58w.
                 :-?8++*wa/!Q-
                 :n/Y615S68
                 :m../*4gq
                 :X_8?T9/*
                 :L57?m/pY!
                 :HL_:W1y/h-6M
                 :yz+*-vb/xtKorS6
                 :Q*/cfG+
                 :!/!J79!
                 :N_/gJH8:43
                 :.hY/xZ*q
                 :DD.:!w-6/Ba?Z?
                 :Y-/zg9Jra-x],
   :nil-key     nil,
   :string-key  "SU7c19Vpf",
   :map-key     {:a -2
                 :b -12951},
   :big-key     "3253659",
   :boolean-key false,
   :int-key     "-1",
   :double-key  0.00823211669921875,
   :keyword-key :-.*+6+s,
   :uuid-key    #uuid "7e9485d0-29cd-422f-adb2-2466bf039b98",
   :any-key     nil
   :extra       "key"
   :additional  {:map ["of" :discarded "data"]}})

(assert (m/validate schema sample-data)
        "The static sample data is valid")

(assert (m/validate
         schema
         (m/decode schema sample-unparsed-data sample-transformer))
        "The sample unparsed data parses correctly")

;; SCHEMA CREATION
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn run-malli-benchmarks!
  "Run each of the benchmarking test cases."
  []
  (binding [crit/*report-warn* true]
    (println "Running Malli Benchmarks")
    (crit/warn-on-suspicious-jvm-options)
    (println "System characteristics: " system-characteristics)
    (print-breaking-line)
    (println "Benchmarking schema creation time")
    (println "Schema variable lookup")
    ; Execution time mean : 0.825787 ns
    ; Execution time std-deviation : 0.121119 ns
    ; Execution time lower quantile : 0.752455 ns ( 2.5%)
    ; Execution time upper quantile : 1.290737 ns (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark schema sample-settings)
       {:verbose true}))
    (print-separating-line)
    (println "Inline schema variable lookup")
    ; Execution time mean : 1.124399 ns
    ; Execution time std-deviation : 0.146815 ns
    ; Execution time lower quantile : 1.058337 ns ( 2.5%)
    ; Execution time upper quantile : 1.706369 ns (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark inlining-lookup sample-settings)
       {:verbose true}))
    (print-separating-line)  
    (println "Compiling a Schema")
    ; Execution time mean : 9.325847 µs
    ; Execution time std-deviation : 222.913025 ns
    ; Execution time lower quantile : 9.119725 µs ( 2.5%)
    ; Execution time upper quantile : 10.127665 µs (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark (m/schema schema) sample-settings)
       {:verbose true}))
    (print-separating-line)
    (println "Generating a Malli validator")
    ; Execution time mean : 12.097368 µs
    ; Execution time std-deviation : 243.971977 ns
    ; Execution time lower quantile : 11.875176 µs ( 2.5%)
    ; Execution time upper quantile : 12.874433 µs (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark (m/validator schema) sample-settings)
       {:verbose true}))
    (print-separating-line)
    (println "Generating a Malli validator from a Malli Schema")
    ; Execution time mean : 26.581133 ns
    ; Execution time std-deviation : 0.748188 ns
    ; Execution time lower quantile : 26.229464 ns ( 2.5%)
    ; Execution time upper quantile : 29.061469 ns (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark (m/validator schema-as-schema) sample-settings)
       {:verbose true}))
    (print-separating-line)
    (println "Generating a Malli decoder")
    ; Execution time mean : 58.658210 µs
    ; Execution time std-deviation : 1.456854 µs
    ; Execution time lower quantile : 57.372629 µs ( 2.5%)
    ; Execution time upper quantile : 63.909067 µs (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark (m/decoder schema sample-transformer) sample-settings)
       {:verbose true}))
    (print-separating-line)
    (println "Generating a Malli decoder from a Malli Schema")
    ; Execution time mean : 44.430810 µs
    ; Execution time std-deviation : 1.336783 µs
    ; Execution time lower quantile : 43.143548 µs ( 2.5%)
    ; Execution time upper quantile : 48.850333 µs (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark (m/decoder schema-as-schema sample-transformer) sample-settings)
       {:verbose true}))

;; DATA VALIDATION
    (print-breaking-line)
    (println "Benchmarking data validation")
    (println "Calling validate against a schema vector")
    ; Execution time mean : 13.044904 µs
    ; Execution time std-deviation : 247.281178 ns
    ; Execution time lower quantile : 12.870479 µs ( 2.5%)
    ; Execution time upper quantile : 13.948751 µs (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result 
       (crit/quick-benchmark (m/validate schema sample-data)  sample-settings) 
       {:verbose true}))
    (print-separating-line)
    (println "Calling validate against a Malli Schema")
    ; Execution time mean : 706.453477 ns
    ; Execution time std-deviation : 17.251739 ns
    ; Execution time lower quantile : 693.437256 ns ( 2.5%)
    ; Execution time upper quantile : 751.326369 ns (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark (m/validate schema-as-schema sample-data) sample-settings)
       {:verbose true}))
    (print-separating-line)
    (println "Using a pre-computed Malli Validator")
    ; Execution time mean : 660.676955 ns
    ; Execution time std-deviation : 25.641216 ns
    ; Execution time lower quantile : 645.357670 ns ( 2.5%)
    ; Execution time upper quantile : 725.211583 ns (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark (schema-validator sample-data) sample-settings)
       {:verbose true}))

;; DATA COERCION
    (print-breaking-line)
    (println "Benchmarking data coercion")
    (println "Calling decode against a schema vector")
    ; Execution time mean : 62.315691 µs
    ; Execution time std-deviation : 1.857252 µs
    ; Execution time lower quantile : 61.126207 µs ( 2.5%)
    ; Execution time upper quantile : 68.716532 µs (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark (m/decode schema sample-unparsed-data sample-transformer) sample-settings)
       {:verbose true}))
    (print-separating-line)
    (println "Calling decode against a Malli schema")
    ; Execution time mean : 47.147764 µs
    ; Execution time std-deviation : 1.153085 µs
    ; Execution time lower quantile : 46.240139 µs ( 2.5%)
    ; Execution time upper quantile : 51.138834 µs (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark (m/decode schema-as-schema sample-unparsed-data sample-transformer) sample-settings)
       {:verbose true}))
    (print-separating-line)
    (println "Calling decode against a pre-computed decoder")
    ; Execution time mean : 3.477707 µs
    ; Execution time std-deviation : 80.537270 ns
    ; Execution time lower quantile : 3.431332 µs ( 2.5%)
    ; Execution time upper quantile : 3.758423 µs (97.5%)
    ; Overhead used : 4.504571 ns
    (crit/with-progress-reporting
      (crit/report-result
       (crit/quick-benchmark (schema-decoder sample-unparsed-data) sample-settings)
       {:verbose true}))
    (print-breaking-line)
    (println "Benchmarking complete")))

