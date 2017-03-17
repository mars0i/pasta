;; This software is copyright 2016, 2017 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns pasta.core
  (:require [pasta.SimConfig :as cfg]
            [pasta.UI :as ui]
            [clojure.pprint]) ; for *print-right-margin*
  (:gen-class))

;; Always handy at the repl
(defn set-pprint-width 
  "Sets width for pretty-printing with pprint and pp."
  [cols] 
  (alter-var-root 
    #'clojure.pprint/*print-right-margin* 
    (constantly cols)))

(defn -main
  [& args]
  (cfg/record-commandline-args! args) 
  (if (and args (not (:use-gui (:options @cfg/commandline$)))) ; if commandline options, default to no-gui unless use-gui is true
    (cfg/mein args)
    (ui/mein args))) ; otherwise default to gui

; (println (:use-gui (:options @cfg/commandline$)))
