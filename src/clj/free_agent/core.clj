(ns free-agent.core
  (:require [utils.random-utils :as ru])
  (:gen-class))

(def rng (ru/make-rng-print-seed))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
