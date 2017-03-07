(ns pasta.core
  (:require [pasta.SimConfig :as cfg]
            [pasta.UI :as ui]) ; makes Java icon come up no matter what :-(
  (:gen-class))

(defn -main
  [& args]
  (cfg/record-commandline-args! args) 
  (if (and args (not (:use-gui (:options @cfg/commandline$)))) ; if commandline options, default to no-gui unless use-gui is true
    (cfg/mein args)
    (ui/mein args))) ; otherwise default to gui

;; also need a way to send false for -g so I can force nogui

;; attempt to not make Java icon come up for nogui
;; doesn't work
;(defn -main
;  [& args]
;  (cfg/record-commandline-args! args) 
;  (println (:use-gui (:options @cfg/commandline$)))
;  (if (and args (not (:use-gui (:options @cfg/commandline$)))) ; if commandline options, default to no-gui unless use-gui is true
;    (cfg/mein args)
;    (do
;      (require 'pasta.UI)
;      (pasta.UI/mein args)))) ; otherwise default to gui
