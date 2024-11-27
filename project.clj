(defproject gym "0.0.0"
  :description "A bunch of code used for heavy lifting and general training"
  :url "https://github.com/nnichols/gym"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [cheshire "5.13.0"]
                 [clj-http "3.13.0"]
                 [criterium/criterium "0.4.6"]
                 [com.wallbrew/brew-bot "3.4.0"]
                 [dj-marky-markov "0.0.3"]
                 [metosin/malli "0.16.4"]
                 [metosin/spec-tools "0.10.7"]]
  :aliases {"benchmark-malli" ["run" "-m" "gym.benchmarking.malli/run-malli-benchmarks!"]})
