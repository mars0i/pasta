;;; This software is copyright 2015 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

(ns free-agent.UI
  (:require [free-agent.SimConfig :as cfg])
  (:import [free-agent SimConfig]
           [sim.engine Steppable Schedule]
           [sim.portrayal.grid ObjectGridPortrayal2D]
           [sim.portrayal.simple OvalPortrayal2D OrientedPortrayal2D]
           [sim.display Console Display2D]
           [java.awt Color])
  (:gen-class
    :name free-agent.UI
    :extends sim.display.GUIState
    :main true
    :exposes {state {:get getState}}  ; accessor for field in superclass
    :exposes-methods {start superStart, quit superQuit, init superInit, getInspector superGetInspector}
    :state iState
    :init init-instance-state))

(defn -init-instance-state
  [& args]
;; TODO HERE: get access to sim-config, then config-data.  
;; Get the mushroom and snipe fields from these, somehow.  
  (let [snipe-field-portrayal (ObjectGridPortrayal2D.)
        mush-field-portrayal (ObjectGridPortrayal2D.)]
    [(vec args) {:display (atom nil)
                 :display-frame (atom nil)
                 :popenv nil
                 :snipe-field-portrayal snipe-field-portrayal
                 :mush-field-portrayal mush-field-portrayal}]))

;; getName()
;; Obscure corner of the already obscure gen-class corner: When a method has multiple arities in the super,
;; you have to distinguish them by tacking type specifiers on to the name of the method.
;; https://groups.google.com/forum/#!topic/clojure/TVRsy4Gnf70
;; https://puredanger.github.io/tech.puredanger.com/2011/08/12/subclassing-in-clojure (in which Alex Miller of all people learns from random others)
;; http://stackoverflow.com/questions/32773861/clojure-gen-class-for-overloaded-and-overridden-methods
;; http://dishevelled.net/Tricky-uses-of-Clojure-gen-class-and-AOT-compilation.html
(defn -getName-void [this] "free-agent") ; override method in super. should cause this to be displayed as title of config window of gui, but it doesn't.

(defn get-display [this] @(:display (.iState this)))
(defn set-display [this newval] (reset! (:display (.iState this)) newval))
(defn get-display-frame [this] @(:display-frame (.iState this)))
(defn set-display-frame [this newval] (reset! (:display-frame (.iState this)) newval))
(defn get-field [this] (:field (.iState this)))
(defn get-field-portrayal [this] (:field-portrayal (.iState this)))
;(defn get-soc-net-portrayal [this] (:soc-net-portrayal (.iState this)))
;(defn get-soc-net [this] (:soc-net (.iState this)))
;(defn get-talk-net-portrayal [this] (:talk-net-portrayal (.iState this)))
;(defn get-talk-net [this] (:talk-net (.iState this)))

;; Override methods in sim.display.GUIState so that UI can make graphs, etc.
(defn -getSimulationInspectedObject [this] (.state this))
;; This controls makes the controls for the sim state in the Model tab (and does other things?):
(defn -getInspector [this]
  (let [i (.superGetInspector this)]
    (.setVolatile i true)
    i))

;;;;;;;;;;;;;;;;;;;;

(declare setup-portrayals schedule-talk-links)

(defn -main
  [& args]
  (let [sim-config (SimConfig. (System/currentTimeMillis))]                                         ; CREATE AN INSTANCE OF my SimState
    (cfg/record-commandline-args! args) 
    (when @cfg/commandline (cfg/set-sim-config-data-from-commandline! sim-config cfg/commandline))
    (.setVisible (Console. (free-agent.UI. sim-config)) true)))                        ; THIS IS WHAT CONNECTS THE GUI TO my SimState (I think)

;; This is called by the pause and go buttons when starting from fully stopped.
(defn -start
  [this-gui]
  (.superStart this-gui) ; this will call start() on the sim, i.e. in our SimState object
  (setup-portrayals this-gui))

(defn setup-portrayals
  [this-gui]  ; instead of 'this': avoid confusion with e.g. proxy below
  (let [sim-config (.getState this-gui)
        rng (.random sim-config)
        field (get-field this-gui)
        field-portrayal (get-field-portrayal this-gui)
        ;soc-net (get-soc-net this-gui)
        ;soc-net-portrayal (get-soc-net-portrayal this-gui)
        ;talk-net (get-talk-net this-gui)
        ;talk-net-portrayal (get-talk-net-portrayal this-gui)
        display (get-display this-gui)
        ;communities (cfg/get-communities sim)
        ;population (cfg/get-population sim)
        ;indiv-portrayal (OrientedPortrayal2D.  ; what this represents is set in the Oriented2D part of Indiv in Sim.clj
        ;                  (proxy [OvalPortrayal2D] [1.5]    ; note proxy auto-captures 'this'
        ;                    (draw [indiv graphics info]                      ; override OvalPortrayal2D method
        ;                      (let [shade (int (* (.getRelig indiv) 255))]  ; UPDATE COLOR FROM DATA IN INDIV
        ;                        (set! (.-paint this) (Color. shade 0 (- 255 shade))) ; paint var is in OvalPortrayal2D
        ;                        (proxy-super draw indiv graphics info))))
        ;                  0 1.75 (Color. 255 175 175) OrientedPortrayal2D/SHAPE_LINE) ; color is of orientation line/shape
        ;soc-edge-portrayal (SimpleEdgePortrayal2D. (Color. 150 150 150) nil)
        ;talk-edge-portrayal (SimpleEdgePortrayal2D. (Color. 200 225 150 85) nil)
        ]
    ;; set up node display
    (.clear field)

    ;(.setField snipe-field-portrayal snipe-field)
    ;(.setField mush-field-portrayal mush-field)

    ;(lay/set-indiv-locs! rng
    ;                     (if (= (.getLinkStyle sim) cfg/sequential-link-style-idx) 0.0 lay/indiv-position-jitter) ; jitter makes it easier to distinguish links that just happen to cross a node
    ;                     field
    ;                     communities)
    ;(.setPortrayalForClass field-portrayal free-agent.Sim.Indiv indiv-portrayal)
    ;; set up within-community social network link display:
    ;(.clear soc-net)
    ;(lay/set-links! soc-net population) ; set-links! sets edges' info fields to nil (null): edges have no weight, so weight defaults to 1.0
    ;(.setShape soc-edge-portrayal SimpleEdgePortrayal2D/SHAPE_LINE_BUTT_ENDS) ; Default SHAPE_THIN_LINE doesn't allow changing thickness. Other differences don't matter, if thinner than nodes.
    ;(.setBaseWidth soc-edge-portrayal 0.15) ; line width
    ;(.setPortrayalForAll soc-net-portrayal soc-edge-portrayal)
    ;; set up actual communication network link display (links added transiently during ticks):
    ;(.clear talk-net)
    ;(.setShape talk-edge-portrayal SimpleEdgePortrayal2D/SHAPE_TRIANGLE)
    ;(.setBaseWidth talk-edge-portrayal 0.85) ; width at base (from end) of triangle
    ;(.setPortrayalForAll talk-net-portrayal talk-edge-portrayal)
    ;; set up display
    (doto display
      (.reset )
      (.setBackdrop (Color. 10 10 10)) ; almost black
      (.repaint))))

(defn -init
  [this controller] ; controller is called c in Java version
  (.superInit this controller)
  (let [display (Display2D. 800 600 this)
        display-frame (.createFrame display)]
    (set-display this display)
    (doto display
      (.setClipping false)
      ;(.attach (get-soc-net-portrayal this) "local networks") ; The order of attaching is the order of painting.
      ;(.attach (get-talk-net-portrayal this) "communications") ; what's attached later will appear on top of what's earlier. 
      ;(.attach (get-field-portrayal this) "indivs")
      )
    ;; set up display frame:
    (set-display-frame this display-frame)
    (.registerFrame controller display-frame)
    (doto display-frame 
      (.setTitle "free-agent")
      (.setVisible true))))

(defn -quit
  [this]
  (.superQuit this)  ; combine in doto?
  (when-let [display-frame (get-display-frame this)]
    (.dispose display-frame))
  (doto this
    (set-display-frame nil)
    (set-display nil)))

(defn repl-gui
  "Convenience function to init and start GUI from the REPL.
  Returns the new Sim object."
  []
  (let [sim-config (SimConfig. (System/currentTimeMillis))]
    (.setVisible (Console. (free-agent.UI. sim-config))
                 true)
    sim-config))
