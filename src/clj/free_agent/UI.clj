;;; This software is copyright 2015 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

(ns free-agent.UI
  (:require [free-agent.SimConfig :as cfg]
            [free-agent.snipe]) ; needed??

  (:import [free-agent SimConfig]

           ;; none of these are working, even if aot'ed:
           [free-agent.snipe]
           ;[free-agent snipe]
           ;[free-agent.snipe.KSnipe]
           ;[free-agent.snipe.KSnipe]
           ;[free-agent.snipe.RSnipe]
           ;[free-agent.snipe RSnipe]
           [free-agent.mushroom]
           ;[free-agent mushroom]
           ;[free-agent.mushroom.Mushroom]
           ;[free-agent.mushroom Mushroom]

           [sim.engine Steppable Schedule]
           [sim.portrayal.grid ObjectGridPortrayal2D]
           [sim.portrayal.simple OvalPortrayal2D OrientedPortrayal2D]
           [sim.display Console Display2D]
           [java.awt Color])
  (:gen-class
    :name free-agent.UI
    :extends sim.display.GUIState
    :main true
    :exposes {state {:get getState}}  ; accessor for field in superclass that will contain my SimConfig after main creates instances of this class with it.
    :exposes-methods {start superStart, quit superQuit, init superInit, getInspector superGetInspector}
    :state uiState
    :init init-instance-state))

(defn -init-instance-state
  [& args]
  [(vec args) {:display (atom nil)       ; will be replaced in init because we need to pass the UI instance to it
               :display-frame (atom nil) ; will be replaced in init because we need to pass the display to it
               :snipe-field-portrayal (ObjectGridPortrayal2D.)
               :mushroom-field-portrayal (ObjectGridPortrayal2D.)}])

;; see doc/getName.md
(defn -getName-void [this] "free-agent") ; override method in super. should cause this to be displayed as title of config window of gui, but it doesn't.

(defn get-display [this] @(:display (.uiState this)))
(defn set-display! [this newval] (reset! (:display (.uiState this)) newval))
(defn get-display-frame [this] @(:display-frame (.uiState this)))
(defn set-display-frame! [this newval] (reset! (:display-frame (.uiState this)) newval))
;(defn get-field [this] (:field (.uiState this)))
(defn get-snipe-field-portrayal [this] (:snipe-field-portrayal (.uiState this)))
(defn get-mushroom-field-portrayal [this] (:mushroom-field-portrayal (.uiState this)))

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
  (let [sim-config (SimConfig. (System/currentTimeMillis))]  ; CREATE AN INSTANCE OF my SimConfig
    (cfg/record-commandline-args! args) 
    (when @cfg/commandline (cfg/set-sim-config-data-from-commandline! sim-config cfg/commandline))
    (.setVisible (Console. (free-agent.UI. sim-config)) true)))  ; THIS IS WHAT CONNECTS THE GUI TO my SimState subclass SimConfig

;; This is called by the pause and go buttons when starting from fully stopped.
(defn -start
  [this-ui]
  (.superStart this-ui) ; this will call start() on the sim, i.e. in our SimState object
  (setup-portrayals this-ui))

;; Possibly also define a load() method. See manual.

(defn setup-portrayals
  [this-ui]  ; instead of 'this': avoid confusion with e.g. proxy below
  (let [sim-config (.getState this-ui)
        rng (.random sim-config)
        cfg-data @(.simConfigData this-ui)
        popenv (:popenv cfg-data)
        snipe-field (:snipe-field popenv)
        mushroom-field  (:mushroom-field  popenv)
        snipe-field-portrayal (get-snipe-field-portrayal this-ui)
        mushroom-field-portrayal (get-mushroom-field-portrayal this-ui)
        display (get-display this-ui)]
    (.clear mushroom-field)
    (.clear snipe-field)
    (.setField snipe-field-portrayal snipe-field)
    (.setField mushroom-field-portrayal mushroom-field)

    (.setPortrayalForClass snipe-field-portrayal free-agent.snipe.KSnipe (OvalPortrayal2D.)) ; need to fix/vary oval portrayal
    (.setPortrayalForClass snipe-field-portrayal free-agent.snipe.RSnipe (OvalPortrayal2D.))
    (.setPortrayalForClass mushroom-field-portrayal free-agent.mushroom.Mushroom (OvalPortrayal2D.))

    ;; set up display
    (doto display
      (.reset )
      (.setBackdrop (Color. 10 10 10)) ; almost black
      (.repaint))))

(defn -init
  [this controller] ; fyi controller is called c in Java version
  (.superInit this controller)
  (let [sim-config (.getState this)
        cfg-data @(.simConfigData this) ; just for world dimensions
        display (Display2D. (:world-width cfg-data) (:world-height cfg-data) this)
        display-frame (.createFrame display)]
    (set-display! this display)
    (doto display
      (.setClipping false)
      (.attach (get-mushroom-field-portrayal this) "mushrooms") ; The order of attaching is the order of painting.
      (.attach (get-snipe-field-portrayal this) "snipes"))  ; what's attached later will appear on top of what's earlier. 
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
