(ns free-agent.snipe
  (:require [free-agent.level :as l])
  (:import [sim.util Proxiable])
  (:gen-class                 ; so it can be aot-compiled
     :name free-agent.snipe)) ; without :name other aot classes won't find it

;; PROOF OF CONCEPT FOR INSPECTOR PROXIES
;; NOT EFFICIENT, NOT COHERENT, AND NOT NECESSARY
;; REAL NEED IS FOR A FUNCTIONAL IMPLEMENTATION OF SNIPES.
;; DOESN'T WORK IN THE END, SOMETHING'S MAKING SNIPES INTO MAPS,
;; WHICH THEN DON'T WORK.

(declare next-id make-k-snipe make-r-snipe is-k-snipe? is-r-snipe?)

(defprotocol SnipeP
  (get-id [this])
  (get-energy [this])
  (get-x [this])
  (get-y [this])
  (set-energy! [this new-val])
  (set-x! [this new-val])
  (set-y! [this new-val]))

;; Does gensym avoid the bottleneck??
(defn next-id 
  "Returns a unique integer for use as an id."
  [] 
  (Long. (str (gensym ""))))

(defprotocol InspectedSnipeP
  (getEnergy [this])
  (getX [this])
  (getY [this]))

;; An inspector proxy that will go out and get the current snipe for a given id and return its data
(defrecord SnipeNow [serialVersionUID id cfg-data$] ; first arg required by Mason for serialization
  InspectedSnipeP
  (getEnergy [this] (get-energy (some #(and (= id (get-id %)) %) (.elements (:snipe-field (:popenv @cfg-data$)))))) ; a HORRIBLY inefficient method
  (getX [this] (get-x (some #(and (= id (get-id %)) %) (.elements (:snipe-field (:popenv @cfg-data$)))))) ; a HORRIBLY inefficient method
  (getY [this] (get-y (some #(and (= id (get-id %)) %) (.elements (:snipe-field (:popenv @cfg-data$)))))) ; a HORRIBLY inefficient method
  Object
  (toString [this] (str "<SnipeNow #" id ">")))

;; Note levels is a sequence of free-agent.Levels
;; The fields are apparently automatically visible to the MASON inspector system. (!)
(deftype KSnipe [id levels ^:unsynchronized-mutable energy ^:unsynchronized-mutable x ^:unsynchronized-mutable y cfg-data$]
  Proxiable ; for inspectors
  (propertiesProxy [this] (SnipeNow. 1 id cfg-data$))
  SnipeP
  (get-id [this] id)
  (get-energy [this] energy)
  (get-x [this] x)
  (get-y [this] y)
  (set-energy! [this new-val] (set! energy new-val))
  (set-x! [this new-val] (set! x new-val))
  (set-y! [this new-val] (set! y new-val))
  Object
  (toString [this] (str "<KSnipe #" id " energy: " energy ">")))

(deftype RSnipe [id levels ^:unsynchronized-mutable energy ^:unsynchronized-mutable x ^:unsynchronized-mutable y cfg-data$]
  Proxiable ; for inspectors
  (propertiesProxy [this] (SnipeNow. 1 id cfg-data$))
  SnipeP
  (get-id [this] id)
  (get-energy [this] energy)
  (get-x [this] x)
  (get-y [this] y)
  (set-energy! [this new-val] (set! energy new-val))
  (set-x! [this new-val] (set! x new-val))
  (set-y! [this new-val] (set! y new-val))
  Object
  (toString [this] (str "<RSnipe #" id " energy: " energy ">")))

(defn make-k-snipe 
  ([cfg-data$ x y]
   (let [{:keys [initial-energy k-snipe-prior]} @cfg-data$]
     (make-k-snipe initial-energy k-snipe-prior x y cfg-data$)))
  ([energy prior x y cfg-data$]
   (KSnipe. (next-id)
            nil ;; TODO construct levels function here using prior
            energy
            x y
            cfg-data$)))

(defn make-r-snipe
  ([cfg-data$ x y]
   (let [{:keys [initial-energy r-snipe-low-prior r-snipe-high-prior]} @cfg-data$]
     (make-r-snipe initial-energy r-snipe-low-prior r-snipe-high-prior x y cfg-data$)))
  ([energy low-prior high-prior x y cfg-data$]
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

