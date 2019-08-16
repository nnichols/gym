(defproject gym "0.0.0"
  :description "A bunch of code used for heavy lifting and general training"
  :url "https://github.com/nnichols/gym"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [dj-marky-markov "0.0.3"]]
  :main ^:skip-aot gym.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
