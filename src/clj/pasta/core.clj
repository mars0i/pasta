(ns pasta.core
  (:require [pasta.SimConfig :as cfg]
            [pasta.UI :as ui])
  (:gen-class))

(defn -main
  [& args]
  (cfg/record-commandline-args! args) 
  (println (:use-gui (:options @cfg/commandline$)))
  (if args
    (if (:use-gui (:options @cfg/commandline$)) ; if commandline options, default to no-gui unless use-gui is true
      (ui/mein args)
      (cfg/mein args))
    (ui/mein args))) ; otherwise default to gui
