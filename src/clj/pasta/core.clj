(ns pasta.core
  (:require [pasta.SimConfig]
            [pasta.UI])
  (:gen-class))

(defn -main
  [& args]
  (if args
    (pasta.SimConfig/mein args)
    (pasta.UI/mein args)))
