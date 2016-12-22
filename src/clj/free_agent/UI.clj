;;; This software is copyright 2015 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

(ns free-agent.UI
  (:require [free-agent.SimConfig :as cfg]
            [clojure.math.numeric-tower :as math])
  (:import [free-agent mush snipe SimConfig]
           [sim.engine Steppable Schedule]
           ;[sim.portrayal.grid ObjectGridPortrayal2D]
           [sim.portrayal.grid HexaObjectGridPortrayal2D]
           ;[sim.portrayal.grid FastHexaObjectGridPortrayal2D]
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
               :snipe-field-portrayal (HexaObjectGridPortrayal2D.)
               :mush-field-portrayal (HexaObjectGridPortrayal2D.)}])

;; see doc/getName.md
(defn -getName-void [this] "free-agent") ; override method in super. Should cause this string to be displayed as title of config window of gui, but it doesn't.

(defn get-display [this] @(:display (.uiState this)))
(defn set-display! [this newval] (reset! (:display (.uiState this)) newval))
(defn get-display-frame [this] @(:display-frame (.uiState this)))
(defn set-display-frame! [this newval] (reset! (:display-frame (.uiState this)) newval))
(defn get-snipe-field-portrayal [this] (:snipe-field-portrayal (.uiState this)))
(defn get-mush-field-portrayal [this] (:mush-field-portrayal (.uiState this)))

;; Override methods in sim.display.GUIState so that UI can make graphs, etc.
(defn -getSimulationInspectedObject [this] (.state this))
;; This makes the controls for the sim state in the Model tab (and does other things?):
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
        cfg-data @(.simConfigData sim-config)
        popenv (:popenv cfg-data)
        snipe-field (:snipe-field popenv)
        mush-field  (:mush-field  popenv)
        snipe-field-portrayal (get-snipe-field-portrayal this-ui)
        mush-field-portrayal (get-mush-field-portrayal this-ui)
        display (get-display this-ui)]
    (.setField mush-field-portrayal mush-field)
    (.setField snipe-field-portrayal snipe-field)
    ;(.setGridLines snipe-field-portrayal true) ; not lines separating cells, but a rep of the coordinate system
    ;(.setBorder snipe-field-portrayal true) ;(.setBorder mush-field-portrayal true)
    ; **NOTE** UNDERSCORES NOT HYPHENS IN CLASSNAMES HERE:
    (.setPortrayalForClass mush-field-portrayal free_agent.mush.Mush (OvalPortrayal2D. (Color. 150 150 150) 1.0))
    (.setPortrayalForNull  mush-field-portrayal (OvalPortrayal2D. (Color. 120 80 80) 1.0)) ; background circle displayed in mushroom-less patches
    (.setPortrayalForClass snipe-field-portrayal free_agent.snipe.KSnipe (OvalPortrayal2D. (Color. 200 0 0) 0.5))
    (.setPortrayalForClass snipe-field-portrayal free_agent.snipe.RSnipe (OvalPortrayal2D. (Color. 0 0 230) 0.5))
    ;; set up display:
    (doto display
      (.reset )
      (.setBackdrop (Color. 0 0 0))
      (.repaint))))

;; This creates a subclass of OvalPortrayal with size 1.0 and random gray colors
;(proxy [OvalPortrayal2D] [1.0]             ; note proxy auto-captures 'this'
;  (draw [indiv graphics info]              ; override OvalPortrayal2D method
;    (let [gray-val (ran/rand-idx rng 256)]
;      (set! (.-paint this) (Color. gray-val gray-val gray-val))
;      (proxy-super draw indiv graphics info))))



;; For hex grid, need to rescale display (based on HexaBugsWithUI.java around line 200 in Mason 19):
(defn hex-scale-height
  [height]
  (+ 0.5 height))
(defn hex-scale-width
  [width] 
  (* (/ 2.0 (math/sqrt 3)) 
     (+ 1 (* (- width 1)
             (/ 3.0 4.0)))))

(defn -init
  [this controller] ; fyi controller is called c in Java version
  (.superInit this controller)
  (let [sim-config (.getState this)
        cfg-data @(.simConfigData sim-config) ; just for world dimensions
        display-size (:world-display-size cfg-data)
        width (int (* display-size (:world-width cfg-data)))
        height (int (* display-size (:world-height cfg-data)))
        width (hex-scale-width width)    ; for hexagonal grid
        height (hex-scale-height height) ; for hexagonal grid
        display (Display2D. width height this)
        display-frame (.createFrame display)]
    (set-display! this display)
    (doto display
      (.setClipping false)
      (.attach (get-mush-field-portrayal this) "mushrooms") ; The order of attaching is the order of painting.
      (.attach (get-snipe-field-portrayal this) "snipes"))  ; what's attached later will appear on top of what's earlier. 
    (set-display-frame! this display-frame)
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
    (set-display-frame! nil)
    (set-display! nil)))

(defn repl-gui
  "Convenience function to init and start GUI from the REPL.
  Returns the new Sim object."
  []
  (let [sim-config (SimConfig. (System/currentTimeMillis))]
    (.setVisible (Console. (free-agent.UI. sim-config))
                 true)
    sim-config))
