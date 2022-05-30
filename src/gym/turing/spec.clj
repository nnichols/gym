(ns gym.turing.spec
  (:require [clojure.spec.alpha :as csa]
            [clojure.string :as str]
            [spec-tools.core :as spec-tools]))

(csa/def ::name
  (spec-tools/spec
   {:type        :string
    :spec        (csa/and string?
                          #(not (str/blank? %)))
    :description "The name of the state"}))

(csa/def ::match?
  (spec-tools/spec
   {:type        :function
    :spec        fn?
    :description "A function that takes a state and a signal and returns true if the state transition the signal"}))

(csa/def ::action
  (spec-tools/spec
   {:type        :function
    :spec        fn?
    :description "A function that takes a state and a signal and returns a new state"}))

(csa/def ::transition
  (spec-tools/spec
   {:type :map
    :description "A transition function from one state to another"
    :spec (csa/keys :req-un [::name
                             ::match?
                             ::resulting-state]
                    :opt-un [::description
                             ::action])}))

(csa/def ::transitions
  (spec-tools/spec
   {:type        :vector
    :spec        (csa/coll-of #(csa/valid? ::transition %))
    :description "A list of legal transitions from the state"}))

(csa/def ::description
  (spec-tools/spec
   {:type        :string
    :spec        (csa/and string?
                          #(not (str/blank? %)))
    :description "The description of the state"}))

(csa/def ::on-enter
  (spec-tools/spec
   {:type        :function
    :description "A function to be called when the state is entered"}))

(csa/def ::on-leave
  (spec-tools/spec
   {:type        :function
    :description "A function to be called when the state is left"}))

(csa/def ::guard
  (spec-tools/spec
   {:type        :function
    :description "A function that returns true if the state-transition is legal"}))

(csa/def ::state
  (spec-tools/spec
   {:type :map
    :description "A single state a State Machine can be in"
    :spec (csa/keys :req-un [::name
                             ::transitions]
                    :opt-un [::description
                             ::on-enter
                             ::on-leave
                             ::guard])}))

(csa/def ::states
  (spec-tools/spec
   {:type        :map
    :description "A list of states a State Machine can be in"
    :spec        (csa/map-of keyword? #(csa/valid? ::state %))}))

(csa/def ::current-state
  (spec-tools/spec
   {:type :map
    :description "The current state of the State Machine"
    :spec (csa/keys :req-un [::name
                             ::transitions]
                    :opt-un [::description
                             ::on-enter
                             ::on-leave
                             ::guard])}))

(csa/def ::state-machine
  (spec-tools/spec
   {:type :map
    :description "A state machine"
    :spec (csa/keys :req-un [::states
                             ::current-state]
                    :opt-un [::description])}))