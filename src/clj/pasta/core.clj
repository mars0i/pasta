;; This software is copyright 2016, 2017, 2018, 2019 by Marshall Abrams, 
;; and is distributed under the Gnu General Public License version 3.0 
;; as specified in the the file LICENSE.

;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clojure main function, and other central utility functions
(ns pasta.core
  (:require [pasta.Sim :as sim]
            [pasta.GUI :as gui]
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
  "This function, core/main, examines the use-gui command line option, if
  it exists, to decide whether to run the main in Sim.clj or the main in
  GUI.clj (which will eventually run code in Sim)."
  [& args]
  ;; The Sim isn't available yet, so store commandline args for later access by start():
  (sim/record-commandline-args! args) ; defined by defsim: records args in commandline$, defined above
  (if (and args (not (:use-gui (:options @sim/commandline$)))) ; if commandline options, default to no-gui unless use-gui is true
    (sim/mein args)  ; a wrapper around sim/main
    (gui/mein args))) ; otherwise default to gui
