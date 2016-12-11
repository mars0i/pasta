(ns free-agent.snipe
  (:require ;[free-agent.SimConfig :as cfg]
            [free-agent.level :as l]))

(defrecord Snipe [id levels energy]) ; levels is a sequence of free-agent.Levels

;; Needed??
;(defprotocol SnipeP
;  (getId [this])
;  (getLevels [this])
;  (getEnergy [this]))
;(defrecord Snipe [id levels energy]
;  SnipeP
;  (getId [this] (:id this))
;  (getLevels [this] (:levels this))
;  (getEnergy [this] (:energy this)))


;; Could be a bottleneck if reproduction starts happening in different threads.
;; In that case I could switch to e.g. (str (gensym "i")) as in intermmitran.
(def prev-snipe-id (atom -1)) ; first inc'ed value will be 0

;; TODO revise to pass in config data from SimcConfig
(defn make-k-snipe []
  (Snipe. (swap! prev-snipe-id inc) 
          nil ;; TODO construct levels here using params from sim state (same for every k-snipe) (or pass in parent and clone it?)
          (s/getInitialSnipeEnergy)))

(defn make-r-snipe [prior]
  (Snipe. (swap! prev-snipe-id inc) 
          nil ;; TODO construct levels here using prior (one of two values)
          ;; note can't just clone parent since there's developmental variation
          (s/getInitialSnipeEnergy)))

