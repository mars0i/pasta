;;; This software is copyright 2015 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.



(ns free-agent.UI
  (:require [free-agent.SimConfig :as cfg]
            [clojure.math.numeric-tower :as math])
  (:import [free-agent mush snipe SimConfig]
           [sim.engine Steppable Schedule Stoppable]
           [sim.field.grid ObjectGrid2D] ; normally doesn't belong in UI: a hack to use a field portrayal to display a background pattern
           [sim.portrayal DrawInfo2D SimpleInspector]
           [sim.portrayal.grid HexaObjectGridPortrayal2D]; FastHexaObjectGridPortrayal2D ObjectGridPortrayal2D
           [sim.portrayal.simple OvalPortrayal2D RectanglePortrayal2D HexagonalPortrayal2D CircledPortrayal2D ShapePortrayal2D]
           [sim.display Console Display2D]
           [java.awt.geom Rectangle2D$Double] ; note wierd Clojure syntax for Java static nested class
           [java.awt Color])
  (:gen-class
    :name free-agent.UI
    :extends sim.display.GUIState
    :main true
    :exposes {state {:get getState}}  ; accessor for field in superclass that will contain my SimConfig after main creates instances of this class with it.
    :exposes-methods {start superStart, quit superQuit, init superInit, getInspector superGetInspector}
    :state getUIState
    :init init-instance-state))

;; display parameters:
(def mush-pos-nutrition-shade 255) ; a grayscale value in [0,255]
(def mush-neg-nutrition-shade 215) ; for white background
;(def mush-neg-nutrition-shade 140) ; for black background
(defn mush-color-fn [shade] (Color. shade (int (* 0.8 shade)) (int (* 0.3 shade))))
(def mush-high-size-appearance 1.0) ; we don't scale mushroom size to modeled size, but
(def mush-low-size-appearance 0.875) ; we display the low-size mushroom smaller
;; white background:
(def bg-color (Color. 255 255 255))   ; color of background without grid (if show-grid is false)
(def display-backdrop-color (Color. 64 64 64)) ; border around subenvs
;; black background
;(def bg-color (Color. 0 0 0))   ; color of background without grid (if show-grid is false)
;(def display-backdrop-color (Color. 70 70 70))
(def subenv-gap 5)
(def snipe-size 0.55)
(defn snipe-shade-fn [max-energy snipe] (int (+ 64 (* 190 (/ (:energy snipe) max-energy)))))
(defn k-snipe-color-fn [max-energy snipe] (Color. (snipe-shade-fn max-energy snipe) 0 0))
(defn r-snipe-color-fn [max-energy snipe] (Color. 0 0 (snipe-shade-fn max-energy snipe)))
(defn s-snipe-color-fn [max-energy snipe] (Color. 0 (snipe-shade-fn max-energy snipe) 0))
(def org-offset 0.6) ; with simple hex portrayals to display grid, organisms off center; pass this to DrawInfo2D to correct.

; DEBUG:
;(defn snipe-shade-fn [max-energy snipe] 
;  (let [shade (int (+ 54 (* 200 (/ (:energy snipe) max-energy))))]
;    (when (> shade 255)
;      (println "SHADE:" shade (dissoc snipe :cfg-data$)))
;    shade))


(defn -init-instance-state
  [& args]
  [(vec args) {:display (atom nil)       ; will be replaced in init because we need to pass the UI instance to it
               :display-frame (atom nil) ; will be replaced in init because we need to pass the display to it
               :bg-field-portrayal (HexaObjectGridPortrayal2D.) ; can be used to put a background or grid only under subenvs
               :west-snipe-field-portrayal (HexaObjectGridPortrayal2D.)
               :west-mush-field-portrayal (HexaObjectGridPortrayal2D.)
               :east-snipe-field-portrayal (HexaObjectGridPortrayal2D.)
               :east-mush-field-portrayal (HexaObjectGridPortrayal2D.)}])

;; see doc/getName.md
(defn -getName-void [this] "free-agent") ; override method in super. Should cause this string to be displayed as title of config window of gui, but it doesn't.

;; Override methods in sim.display.GUIState so that UI can make graphs, etc.
(defn -getSimulationInspectedObject [this] (.state this))
;; This makes the controls for the sim state in the Model tab (and does other things?):
(defn -getInspector [this]
  (let [i (.superGetInspector this)]
    (.setVolatile i true)
    i))

;;;;;;;;;;;;;;;;;;;;

(declare setup-portrayals)

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

(defn make-fnl-circled-portrayal
  "Create a subclass of CircledPortrayal2D that tracks snipes by id
  rather than by pointer identity."
  [child-portrayal color]
  (proxy [CircledPortrayal2D] [child-portrayal color false]
    (draw [snipe graphics info]
      (.setCircleShowing this @(:circled$ snipe))
      (proxy-super draw snipe graphics info))))

;; TODO abstract out some of the repetition below
(defn setup-portrayals
  [this-ui]  ; instead of 'this': avoid confusion with e.g. proxy below
  (let [sim-config (.getState this-ui)
        ui-config (.getUIState this-ui)
        cfg-data$ (.simConfigData sim-config)
        rng (.random sim-config)
        cfg-data @cfg-data$
        west-popenv (:west-popenv cfg-data)
        east-popenv (:east-popenv cfg-data)
        max-energy (:max-energy cfg-data)
        birth-threshold (:birth-threshold cfg-data)
        mush-high-size (:mush-high-size cfg-data)
        display @(:display ui-config)
        ;; These portrayals should be local to setup-portrayals because 
        ;; proxy needs to capture the correct 'this', and we need cfg-data:
        mush-portrayal (proxy [OvalPortrayal2D] []
                         (draw [mush graphics info]  ; override method in super
                           (let [size  (if (= mush-high-size (:size mush)) mush-high-size-appearance mush-low-size-appearance)
                                 shade (if (neg? (:nutrition mush)) mush-neg-nutrition-shade mush-pos-nutrition-shade)]
                             (set! (.-scale this) size)                       ; superclass vars
                             (set! (.-paint this) (mush-color-fn shade))
                             (proxy-super draw mush graphics (DrawInfo2D. info org-offset org-offset))))) ; last arg centers organism in hex cell
        r-snipe-portrayal-pref-small (make-fnl-circled-portrayal 
                                       (proxy [ShapePortrayal2D] [ShapePortrayal2D/X_POINTS_TRIANGLE_DOWN 
                                                                  ShapePortrayal2D/Y_POINTS_TRIANGLE_DOWN
                                                                  (* 1.1 snipe-size)]
                                         (draw [snipe graphics info] ; orverride method in super
                                           (set! (.-paint this) (r-snipe-color-fn (min max-energy birth-threshold) snipe)) ; paint var is in superclass
                                           (proxy-super draw snipe graphics (DrawInfo2D. info (* 0.75 org-offset) (* 0.55 org-offset))))) ; see above re last arg
                                       Color/blue)
        r-snipe-portrayal-pref-big (make-fnl-circled-portrayal 
                                     (proxy [ShapePortrayal2D] [ShapePortrayal2D/X_POINTS_TRIANGLE_UP 
                                                                ShapePortrayal2D/Y_POINTS_TRIANGLE_UP
                                                                (* 1.1 snipe-size)]
                                       (draw [snipe graphics info] ; orverride method in super
                                         (set! (.-paint this) (r-snipe-color-fn (min max-energy birth-threshold) snipe)) ; paint var is in superclass
                                         (proxy-super draw snipe graphics (DrawInfo2D. info (* 0.75 org-offset) (* 0.55 org-offset))))) ; see above re last arg
                                     Color/blue)
        k-snipe-portrayal (make-fnl-circled-portrayal 
                            (proxy [OvalPortrayal2D] [(* 1.1 snipe-size)]
                              (draw [snipe graphics info] ; override method in super
                                (set! (.-paint this) (k-snipe-color-fn max-energy snipe)) ; superclass var
                                (proxy-super draw snipe graphics (DrawInfo2D. info org-offset org-offset)))) ; see above re last arg
                            Color/red)
        s-snipe-portrayal (make-fnl-circled-portrayal 
                            (proxy [RectanglePortrayal2D] [(* 0.915 snipe-size)] ; squares need to be bigger than circles
                              (draw [snipe graphics info] ; orverride method in super
                                (set! (.-paint this) (s-snipe-color-fn (min max-energy birth-threshold) snipe)) ; paint var is in superclass
                                (proxy-super draw snipe graphics (DrawInfo2D. info (* 1.5 org-offset) (* 1.5 org-offset))))) ; see above re last arg
                            Color/black)
        bg-field-portrayal (:bg-field-portrayal ui-config)
        west-snipe-field-portrayal (:west-snipe-field-portrayal ui-config)
        east-snipe-field-portrayal (:east-snipe-field-portrayal ui-config)
        west-mush-field-portrayal (:west-mush-field-portrayal ui-config)
        east-mush-field-portrayal (:east-mush-field-portrayal ui-config)]
    ;; connect fields to their portrayals
    (.setField bg-field-portrayal (ObjectGrid2D. (:env-width cfg-data) (:env-height cfg-data)))
    (.setField west-snipe-field-portrayal (:snipe-field west-popenv))
    (.setField west-mush-field-portrayal (:mush-field west-popenv))
    (.setField east-snipe-field-portrayal (:snipe-field east-popenv))
    (.setField east-mush-field-portrayal (:mush-field east-popenv))
    ;; extra field portrayal to set a background color under the subenvs:
    (.setPortrayalForNull bg-field-portrayal (HexagonalPortrayal2D. bg-color 1.2))
    ; **NOTE** UNDERSCORES NOT HYPHENS IN free_agent CLASSNAMES BELOW:
    ;; connect portrayals to agents:
    ;; mushs:
    (.setPortrayalForClass west-mush-field-portrayal free_agent.mush.Mush mush-portrayal)
    (.setPortrayalForClass east-mush-field-portrayal free_agent.mush.Mush mush-portrayal)
    ;; west snipes:
    (.setPortrayalForClass west-snipe-field-portrayal free_agent.snipe.KSnipe k-snipe-portrayal)
    (.setPortrayalForClass west-snipe-field-portrayal free_agent.snipe.RSnipePrefSmall r-snipe-portrayal-pref-small)
    (.setPortrayalForClass west-snipe-field-portrayal free_agent.snipe.RSnipePrefBig   r-snipe-portrayal-pref-big)
    (.setPortrayalForClass west-snipe-field-portrayal free_agent.snipe.SSnipe s-snipe-portrayal)
    ;; east snipes:
    (.setPortrayalForClass east-snipe-field-portrayal free_agent.snipe.KSnipe k-snipe-portrayal)
    (.setPortrayalForClass east-snipe-field-portrayal free_agent.snipe.RSnipePrefSmall r-snipe-portrayal-pref-small)
    (.setPortrayalForClass east-snipe-field-portrayal free_agent.snipe.RSnipePrefBig   r-snipe-portrayal-pref-big)
    (.setPortrayalForClass east-snipe-field-portrayal free_agent.snipe.SSnipe s-snipe-portrayal)
    ;; Since popenvs are updated functionally, have to tell the ui about the new popenv on every timestep:
    (.scheduleRepeatingImmediatelyAfter this-ui
                                        (reify Steppable 
                                          (step [this sim-state]
                                            (let [{:keys [west-popenv east-popenv]} @cfg-data$]
                                              (.setField west-snipe-field-portrayal (:snipe-field west-popenv))
                                              (.setField west-mush-field-portrayal (:mush-field west-popenv))
                                              (.setField east-snipe-field-portrayal (:snipe-field east-popenv))
                                              (.setField east-mush-field-portrayal (:mush-field east-popenv))))))
    ;; set up display:
    (doto display
      (.reset )
      (.setBackdrop display-backdrop-color)
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
        ui-config (.getUIState this)
        cfg-data @(.simConfigData sim-config) ; just for env dimensions
        display-size (:env-display-size cfg-data)
        width  (hex-scale-width  (int (* display-size (:env-width cfg-data))))
        height (hex-scale-height (int (* display-size (:env-height cfg-data))))
        display (Display2D. (+ subenv-gap (* 2 width)) height this)
        display-frame (.createFrame display)
        bg-field-portrayal (:bg-field-portrayal ui-config)
        west-snipe-field-portrayal (:west-snipe-field-portrayal ui-config)
        east-snipe-field-portrayal (:east-snipe-field-portrayal ui-config)
        west-mush-field-portrayal (:west-mush-field-portrayal ui-config)
        east-mush-field-portrayal (:east-mush-field-portrayal ui-config)]
    (reset! (:display ui-config) display)
    (doto display
      (.setClipping false)
      ;; note Clojure $ syntax for Java static nested classes:
      ;;                                                  upper left corner: x     y 
      (.attach bg-field-portrayal     "west background" (Rectangle2D$Double. 0     0 width height))
      (.attach west-snipe-field-portrayal "west snipes" (Rectangle2D$Double. 0     0 width height))
      (.attach west-mush-field-portrayal  "west mushs"  (Rectangle2D$Double. 0     0 width height))
      (.attach bg-field-portrayal     "east background" (Rectangle2D$Double. (+ width subenv-gap) 0 width height)) ; NOTE reusing same portrayal as above
      (.attach east-snipe-field-portrayal "east snipes" (Rectangle2D$Double. (+ width subenv-gap) 0 width height))
      (.attach east-mush-field-portrayal  "east mushs"  (Rectangle2D$Double. (+ width subenv-gap) 0 width height)))
    (reset! (:display-frame ui-config) display-frame)
    (.registerFrame controller display-frame)
    (doto display-frame 
      (.setTitle "free-agent")
      (.setVisible true))))

(defn -quit
  [this]
  (.superQuit this)  ; combine in doto?
  (let [ui-config (.getUIState this)
        display (:display ui-config)
        display-frame (:display-frame ui-config)]
    (when display-frame
      (.dispose display-frame))
    (reset! (:display ui-config) nil)
    (reset! (:display-frame ui-config) nil)))

;; Try this:
;; (let [snipes (.elements (:snipe-field (:popenv @cfg-data$))) N (count snipes) energies (map :energy snipes)] [N (/ (apply + energies) N)])
(defn repl-gui
  "Convenience function to init and start GUI from the REPL.
  Returns the new SimConfig object.  Usage e.g.:
  (use 'free-agent.UI) 
  (let [[cfg ui] (repl-gui)] (def cfg cfg) (def ui ui)) ; considered bad practice--but convenient in this case
  (def data$ (.simConfigData cfg))"
  []
  (let [sim-config (SimConfig. (System/currentTimeMillis))
        ui (free-agent.UI. sim-config)]
    (.setVisible (Console. ui) true)
    [sim-config ui]))

(defmacro repl-gui-with-defs
  "Calls repl-gui to start the gui, then creates top-level definitions:
  cfg as a free-agent.SimConfig (i.e. a SimState), ui as a free-agent.UI
  (i.e. a GUIState) that references cfg, and data$ as an atom containing 
  cfg's SimConfigData stru."
  []
  (let [[cfg ui] (repl-gui)]
    (def cfg cfg)
    (def ui ui))
  (def data$ (.simConfigData cfg))
  (println "cfg is defined as a SimConfig (i.e. a SimState)")
  (println "ui is defined as a UI (i.e. a GUIState)")
  (println "data$ is defined as an atom containing cfg's SimConfigData stru."))
