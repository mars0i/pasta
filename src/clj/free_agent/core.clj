(ns free-agent.core
  (:gen-class))

(def random-seed (ran/make-long-seed))
(def rng (ran/make-rng random-seed))
(println "random-seed:" random-seed)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
