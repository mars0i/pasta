(ns free-agent.snipe
  (:require [free-agent.level :as l])
  (:import [sim.util Properties SimpleProperties Propertied])
  (:gen-class                 ; so it can be aot-compiled
     :name free-agent.snipe)) ; without :name other aot classes won't find it

;; The real difference between k- and r-snipes is in how levels is implemented,
;; but it will be useful to have two different wrapper classes to make it easier to
;; observe differences.

;; Does gensym avoid bottleneck??
(defn next-id 
  "Returns a unique integer for use as an id."
  [] 
  (Long. (str (gensym ""))))

;(defprotocol InspectedSnipeP
;  (getEnergy [this]))

(defn get-current-snipe
  "Returns the snipe in the current PopEnv with id."
  [id cfg-data$]
  ((:snipes (:popenv @cfg-data$))
   id))

;; TODO: add Propertied to RSnipe
;; levels is a sequence of free-agent.Levels
(defrecord KSnipe [id levels energy x y cfg-data$]
  Propertied
  (properties [original-snipe] 
    (proxy [SimpleProperties] [(get-current-snipe id cfg-data$)]
                                 (getObject [properties] original-snipe)
                                 (getDescription [properties i] (["energy"] 0))
                                 (getName [properties i] (["energy"] 0))
                                 (getType [properties i] ([double] i))
                                 (getValue [properties i]
                                   (let [curr-snipe (.-object properties)]
                                     ([(:energy curr-snipe)] i)))
                                 (isHidden [properties i] ([false] i))
                                 (isReadWrite [properties i] ([false] i))
                                 (isVolatile [properties] false)
                                 (numProperties [properties] 1)
                                 (toString [properties] 
                                   (str "SimpleProperties: snipe id=" id " snipe=" (.-object properties)))))
  Object
  (toString [_] (str "<KSnipe #" id " energy: " energy ">")))

;; TODO: add Propertied to RSnipe
(defrecord RSnipe [id levels energy x y cfg-data$]
  Object
  (toString [this] (str "<RSnipe #" id " energy: " energy ">")))

(defn make-k-snipe 
  ([cfg-data$ x y]
   (let [{:keys [initial-energy k-snipe-prior]} @cfg-data$]
     (make-k-snipe cfg-data$ initial-energy k-snipe-prior x y)))
  ([cfg-data$ energy prior x y]
   (KSnipe. (next-id)
            nil ;; TODO construct levels function here using prior
            energy
            x y
            cfg-data$)))

(defn make-r-snipe
  ([cfg-data$ x y]
   (let [{:keys [initial-energy r-snipe-low-prior r-snipe-high-prior]} @cfg-data$]
     (make-r-snipe cfg-data$ initial-energy r-snipe-low-prior r-snipe-high-prior x y)))
  ([cfg-data$ energy low-prior high-prior x y]
   (RSnipe. (next-id)
            nil ;; TODO construct levels function here using prior (one of two values, randomly)
            energy
            x y
            cfg-data$)))

;; note underscores
(defn is-k-snipe? [s] (instance? free_agent.snipe.KSnipe s))
(defn is-r-snipe? [s] (instance? free_agent.snipe.RSnipe s))


;; Incredibly, the following is not needed in order for snipes to be inspectable.
;; MASON simply sees the record fields as properties.
;; Thank you Clojure and MASON.
;;
;;     (defprotocol InspectedSnipe (getEnergy [this]))
;;     (definterface InspectedSnipe (^double getEnergy []))
;;     To see that this method is visible for snipes, try this:
;;     (pprint (.getDeclaredMethods (class k)))
