;;; This software is copyright 2015 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

(ns free-agent.UI
  (:require [free-agent.SimConfig :as cfg]
            [clojure.math.numeric-tower :as math])
  (:import [free-agent mush snipe SimConfig]
           [sim.engine Steppable Schedule]
           [sim.field.grid ObjectGrid2D] ; normally doesn't belong in UI: a hack to use a field portrayal to display a background pattern
           ;[sim.portrayal.grid ObjectGridPortrayal2D]
           [sim.portrayal.grid HexaObjectGridPortrayal2D]
           ;[sim.portrayal.grid FastHexaObjectGridPortrayal2D]
           [sim.portrayal.simple OvalPortrayal2D RectanglePortrayal2D ShapePortrayal2D HexagonalPortrayal2D]
           [sim.display Console Display2D]
           [java.awt Color])
  (:gen-class
    :name free-agent.UI
    :extends sim.display.GUIState
    :main true
    :exposes {state {:get getState}}  ; accessor for field in superclass that will contain my SimConfig after main creates instances of this class with it.
    :exposes-methods {start superStart, quit superQuit, init superInit, getInspector superGetInspector}
    :state getUIState
    :init init-instance-state))

;; fixed display parameters:
(def k-snipe-color (Color. 240 0 0))
(def r-snipe-color (Color. 0 0 250))
(def snipe-size 0.42)
(def mush-pos-nutrition-shade 100) ; a grayscale value in [0,255]
(def mush-neg-nutrition-shade 255)
(def mush-high-mean-size 1.0) ; we don't scale mushroom size to modeled size, but
(def mush-low-mean-size 0.70)  ;  still display the low-size mushroom smaller
;; background circle displayed in mushroom-less patches:
(def bg-pattern-color (Color. 200 200 200))
;(def bg-pattern-color (Color. 200 165 165)) ; a dirty pink

(defn -init-instance-state
  [& args]
  [(vec args) {:display (atom nil)       ; will be replaced in init because we need to pass the UI instance to it
               :display-frame (atom nil) ; will be replaced in init because we need to pass the display to it
               :snipe-field-portrayal (HexaObjectGridPortrayal2D.)
               :mush-field-portrayal  (HexaObjectGridPortrayal2D.)
               :bg-field-portrayal    (HexaObjectGridPortrayal2D.)}]) ; static background pattern

;; see doc/getName.md
(defn -getName-void [this] "free-agent") ; override method in super. Should cause this string to be displayed as title of config window of gui, but it doesn't.

(defn get-display [this] @(:display (.getUIState this)))
(defn set-display! [this newval] (reset! (:display (.getUIState this)) newval))
(defn get-display-frame [this] @(:display-frame (.getUIState this)))
(defn set-display-frame! [this newval] (reset! (:display-frame (.getUIState this)) newval))
(defn get-snipe-field-portrayal [this] (:snipe-field-portrayal (.getUIState this)))
(defn get-mush-field-portrayal [this] (:mush-field-portrayal (.getUIState this)))
(defn get-bg-field-portrayal [this] (:bg-field-portrayal (.getUIState this)))

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

(defn k-snipe-color-fn
  [shade]
  (Color. shade 0 0))

(defn r-snipe-color-fn
  [shade]
  (Color. 0 0 shade))

(defn setup-portrayals
  [this-ui]  ; instead of 'this': avoid confusion with e.g. proxy below
  (let [sim-config (.getState this-ui)
        ui-config (.getUIState this-ui)
        rng (.random sim-config)
        cfg-data$ (.simConfigData sim-config)
        cfg-data @cfg-data$
        max-energy (:max-energy cfg-data)
        mush-high-mean (:mush-high-mean cfg-data)
        popenv (:popenv cfg-data)
        mush-field  (:mush-field  popenv)
        snipe-field (:snipe-field popenv)
        mush-field-portrayal (get-mush-field-portrayal this-ui)
        snipe-field-portrayal (get-snipe-field-portrayal this-ui)
        bg-field-portrayal (get-bg-field-portrayal this-ui)
        display (get-display this-ui)
        mush-portrayal (proxy [OvalPortrayal2D] []   ; note captures 'this'
                         (draw [mush graphics info]  ; override method in super
                           (let [size  (if (= mush-high-mean (:mean mush))  ; btw: why the def has to be local to setup-portrayals
                                         mush-high-mean-size
                                         mush-low-mean-size)
                                 shade (if (neg? (:nutrition mush))
                                         mush-neg-nutrition-shade
                                         mush-pos-nutrition-shade)]
                             (set! (.-scale this) size)
                             (set! (.-paint this) (Color. shade shade shade))
                             (proxy-super draw mush graphics info))))
        make-snipe-portrayal (fn [max-energy color-fn]
                               (proxy [OvalPortrayal2D] [snipe-size]
                                 (draw [snipe graphics info]          ; override OvalPortrayal2D method
                                   (let [shade (int (* 255 (/ (:energy snipe) max-energy)))]
                                     (set! (.-paint this) (color-fn shade)) ; paint var is in OvalPortrayal2D
                                     (proxy-super draw snipe graphics info)))))
        k-snipe-portrayal (make-snipe-portrayal max-energy k-snipe-color-fn)
        r-snipe-portrayal (make-snipe-portrayal max-energy r-snipe-color-fn)]
    (.setField bg-field-portrayal (ObjectGrid2D. (:env-width cfg-data) (:env-height cfg-data))) ; empty field portrayal just to display a background grid
    (.setField mush-field-portrayal mush-field)
    (.setField snipe-field-portrayal snipe-field)

    (.setPortrayalForNull bg-field-portrayal (OvalPortrayal2D. bg-pattern-color 1.0)) ; background circle displayed in mushroom-less patches
    ;(.setPortrayalForNull bg-field-portrayal (HexagonalPortrayal2D. bg-pattern-color 0.925)) ; or hexagons, smaller than cell to show edges

    ; **NOTE** UNDERSCORES NOT HYPHENS IN free_agent CLASSNAMES BELOW:
    (.setPortrayalForClass mush-field-portrayal free_agent.mush.Mush mush-portrayal)

    (.setPortrayalForClass snipe-field-portrayal free_agent.snipe.KSnipe k-snipe-portrayal)
    ;(.setPortrayalForClass snipe-field-portrayal free_agent.snipe.KSnipe (OvalPortrayal2D. k-snipe-color snipe-size))
    ;(.setPortrayalForClass snipe-field-portrayal free_agent.snipe.KSnipe (ShapePortrayal2D. ShapePortrayal2D/X_POINTS_BOWTIE ShapePortrayal2D/Y_POINTS_BOWTIE k-snipe-color snipe-size))

    (.setPortrayalForClass snipe-field-portrayal free_agent.snipe.RSnipe r-snipe-portrayal)
    ;(.setPortrayalForClass snipe-field-portrayal free_agent.snipe.RSnipe (OvalPortrayal2D. r-snipe-color snipe-size))
    ;(.setPortrayalForClass snipe-field-portrayal free_agent.snipe.RSnipe (RectanglePortrayal2D. r-snipe-color snipe-size))

    ;; Since popenvs are updated functionally, have to tell the ui about the new popenv on every timestep:
    (.scheduleRepeatingImmediatelyAfter this-ui
                                        (reify Steppable 
                                          (step [this sim-state]
                                            (let [{:keys [snipe-field mush-field]} (:popenv @cfg-data$)]
                                            (.setField snipe-field-portrayal snipe-field)
                                            (.setField mush-field-portrayal mush-field)))))
    ;; set up display:
    (doto display
      (.reset )
      (.setBackdrop (Color. 0 0 0))
      (.repaint))))



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
        cfg-data @(.simConfigData sim-config) ; just for env dimensions
        display-size (:env-display-size cfg-data)
        width (int (* display-size (:env-width cfg-data)))
        height (int (* display-size (:env-height cfg-data)))
        width (hex-scale-width width)    ; for hexagonal grid
        height (hex-scale-height height) ; for hexagonal grid
        display (Display2D. width height this)
        display-frame (.createFrame display)
        bg-field-portrayal (get-bg-field-portrayal this)
        mush-field-portrayal (get-mush-field-portrayal this)
        snipe-field-portrayal (get-snipe-field-portrayal this)]
    (set-display! this display)
    (doto display
      (.setClipping false)
      (.attach bg-field-portrayal "env")         ; The order of attaching is the order of painting.
      (.attach mush-field-portrayal "mushrooms") ; what's attached later will appear on top of what's earlier. 
      (.attach snipe-field-portrayal "snipes"))  
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
  Returns the new SimConfig object."
  []
  (let [sim-config (SimConfig. (System/currentTimeMillis))]
    (.setVisible (Console. (free-agent.UI. sim-config))
                 true)
    sim-config))
