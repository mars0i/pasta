(ns pasta.core
  (:require [pasta.SimConfig :as cfg]
            [pasta.UI :as ui])
  (:gen-class))

(defn -main
  [& args]
  (cfg/record-commandline-args! args) 
  (if (and args (not (:use-gui (:options @cfg/commandline$)))) ; if commandline options, default to no-gui unless use-gui is true
    (cfg/mein args)
    (ui/mein args))) ; otherwise default to gui

; (println (:use-gui (:options @cfg/commandline$)))
