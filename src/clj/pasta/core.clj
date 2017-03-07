(ns pasta.core
  (:require [pasta.SimConfig :as cfg]) ;[pasta.UI :as ui]
  (:gen-class))

;; also need a way to send false for -g so I can force nogui

(defn -main
  [& args]
  (cfg/record-commandline-args! args) 
  (if (and args (not (:use-gui (:options @cfg/commandline$)))) ; if commandline options, default to no-gui unless use-gui is true
    (cfg/mein args)
    (do
      (require 'pasta.UI)
      (pasta.UI/mein args)))) ; otherwise default to gui

;(defn -main
;  [& args]
;  (cfg/record-commandline-args! args) 
;  (if (and args (not (:use-gui (:options @cfg/commandline$)))) ; if commandline options, default to no-gui unless use-gui is true
;    (cfg/mein args)
;    (ui/mein args))) ; otherwise default to gui
