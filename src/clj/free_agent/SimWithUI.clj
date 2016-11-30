;;; This software is copyright 2015 by Marshall Abrams, and
;;; is distributed under the Gnu General Public License version 3.0 as
;;; specified in the file LICENSE.

;; Note: Traditional MASON models put e.g. Continuous2D and Network in another
;; class that's central to the model.  This class would normally use those
;; instances from the other class, passing them to portrayals created here.
;; Since the underlying model (Sim) doesn't need spatial relations or explicit
;; link representations, I create the Continuous2D and Network here, because
;; they're needed by the portrayals.

(ns intermit.SimWithUI
  (:require [intermit.layout :as lay]
            [intermit.Sim :as s])
  (:import [intermit Sim]
           [sim.engine Steppable Schedule]
           [sim.field.continuous Continuous2D]
           [sim.field.network Network Edge]
           [sim.portrayal.continuous ContinuousPortrayal2D]
           [sim.portrayal.network NetworkPortrayal2D SpatialNetwork2D SimpleEdgePortrayal2D]
           [sim.portrayal.simple OvalPortrayal2D OrientedPortrayal2D]
           [sim.display Console Display2D]
           [java.awt Color])
  (:gen-class
    :name intermit.SimWithUI
    :extends sim.display.GUIState
    :main true
    :exposes {state {:get getState}}  ; accessor for field in superclass
    :exposes-methods {start superStart, quit superQuit, init superInit, getInspector superGetInspector}
    ;:methods []
    :state iState
    :init init-instance-state))

(defn -init-instance-state
  [& args]
  (let [field (Continuous2D. 1.0 125 100)
        field-portrayal (ContinuousPortrayal2D.)
        soc-net (Network. false) ; undirected
        soc-net-portrayal (NetworkPortrayal2D.)
        talk-net (Network. true) ; directed
        talk-net-portrayal (NetworkPortrayal2D.)]
    (.setField field-portrayal field)
    (.setField soc-net-portrayal  (SpatialNetwork2D. field soc-net))
    (.setField talk-net-portrayal (SpatialNetwork2D. field talk-net))
    [(vec args) {:display (atom nil)
                 :display-frame (atom nil)
                 :field field                         ; holds nodes
                 :field-portrayal field-portrayal     ; displays nodes
                 :soc-net soc-net                     ; holds within-community social network
                 :soc-net-portrayal soc-net-portrayal ; displays ditto
                 :talk-net talk-net
                 :talk-net-portrayal talk-net-portrayal}]))

(defn get-display [this] @(:display (.iState this)))
(defn set-display [this newval] (reset! (:display (.iState this)) newval))
(defn get-display-frame [this] @(:display-frame (.iState this)))
(defn set-display-frame [this newval] (reset! (:display-frame (.iState this)) newval))
(defn get-field [this] (:field (.iState this)))
(defn get-field-portrayal [this] (:field-portrayal (.iState this)))
(defn get-soc-net-portrayal [this] (:soc-net-portrayal (.iState this)))
(defn get-soc-net [this] (:soc-net (.iState this)))
(defn get-talk-net-portrayal [this] (:talk-net-portrayal (.iState this)))
(defn get-talk-net [this] (:talk-net (.iState this)))

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
  (let [sim (Sim. (System/currentTimeMillis))]
    (s/record-commandline-args! args) 
    (when @s/commandline (s/set-instance-state-from-commandline! sim s/commandline))
    (.setVisible (Console. (intermit.SimWithUI. sim)) true)))

(defn -getName [this] "Intermittent") ; override method in super

;; This is called by the pause and go buttons when starting from fully stopped.
(defn -start
  [this-gui]
  (.superStart this-gui) ; this will call start() on the sim, i.e. in our SimState object
  (setup-portrayals this-gui)
  (schedule-talk-links this-gui)) ; TODO: Question: SHOULD THIS BE IN INIT??

;; Schedule a step here to transiently add/remove talk-links from the talk-net.
;; This isn't needed in the underlying simulation, so do it here rather than Sim.clj.
(defn schedule-talk-links
  [this-gui]
  (.scheduleRepeating (.schedule (.getState this-gui))
                      Schedule/EPOCH 2
                      (reify Steppable
                        (step [this-steppable sim-state]
                          (let [talk-net (get-talk-net this-gui)
                                istate (.instanceState sim-state)
                                population @(.population istate)]
                            (.clear talk-net)
                            (doseq [indiv population] 
                              (when-let [speaker (s/get-prev-speaker indiv)]
                                (.addEdge talk-net speaker indiv nil))))))))
                                                 ;  from    to  (from end is wider; to end is pointed)

(defn setup-portrayals
  [this-gui]  ; instead of 'this': avoid confusion with proxy below
  (let [sim (.getState this-gui)
        rng (.random sim)
        field (get-field this-gui)
        field-portrayal (get-field-portrayal this-gui)
        soc-net (get-soc-net this-gui)
        soc-net-portrayal (get-soc-net-portrayal this-gui)
        talk-net (get-talk-net this-gui)
        talk-net-portrayal (get-talk-net-portrayal this-gui)
        display (get-display this-gui)
        communities (s/get-communities sim)
        population (s/get-population sim)
        indiv-portrayal (OrientedPortrayal2D.  ; what this represents is set in the Oriented2D part of Indiv in Sim.clj
                          (proxy [OvalPortrayal2D] [1.5]    ; note proxy auto-captures 'this'
                            (draw [indiv graphics info]                      ; override OvalPortrayal2D method
                              (let [shade (int (* (.getRelig indiv) 255))]
                                (set! (.-paint this) (Color. shade 0 (- 255 shade))) ; paint var is in OvalPortrayal2D
                                (proxy-super draw indiv graphics info))))
                          0 1.75 (Color. 255 175 175) OrientedPortrayal2D/SHAPE_LINE) ; color is of orientation line/shape
        soc-edge-portrayal (SimpleEdgePortrayal2D. (Color. 150 150 150) nil)
        talk-edge-portrayal (SimpleEdgePortrayal2D. (Color. 200 225 150 85) nil)]
    ;; set up node display
    (.clear field)
    (lay/set-indiv-locs! rng
                         (if (= (.getLinkStyle sim) s/sequential-link-style-idx) 0.0 lay/indiv-position-jitter) ; jitter makes it easier to distinguish links that just happen to cross a node
                         field
                         communities)
    (.setPortrayalForClass field-portrayal intermit.Sim.Indiv indiv-portrayal)
    ;; set up within-community social network link display:
    (.clear soc-net)
    (lay/set-links! soc-net population) ; set-links! sets edges' info fields to nil (null): edges have no weight, so weight defaults to 1.0
    (.setShape soc-edge-portrayal SimpleEdgePortrayal2D/SHAPE_LINE_BUTT_ENDS) ; Default SHAPE_THIN_LINE doesn't allow changing thickness. Other differences don't matter, if thinner than nodes.
    (.setBaseWidth soc-edge-portrayal 0.15) ; line width
    (.setPortrayalForAll soc-net-portrayal soc-edge-portrayal)
    ;; set up actual communication network link display (links added transiently during ticks):
    (.clear talk-net)
    (.setShape talk-edge-portrayal SimpleEdgePortrayal2D/SHAPE_TRIANGLE)
    (.setBaseWidth talk-edge-portrayal 0.85) ; width at base (from end) of triangle
    (.setPortrayalForAll talk-net-portrayal talk-edge-portrayal)
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
      (.attach (get-soc-net-portrayal this) "local networks") ; The order of attaching is the order of painting.
      (.attach (get-talk-net-portrayal this) "communications") ; what's attached later will appear on top of what's earlier. 
      (.attach (get-field-portrayal this) "indivs"))
    ;; set up display frame:
    (set-display-frame this display-frame)
    (.registerFrame controller display-frame)
    (doto display-frame 
      (.setTitle "Intermittent")
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
  (let [sim (Sim. (System/currentTimeMillis))]
    (.setVisible (Console. (intermit.SimWithUI. sim))
                 true)
    sim))
