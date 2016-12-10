;; on 12/9/2016, 10pm,
;; this:

(def default-initial-snipe-energy 10.0)
(def default-r-snipe-prior-0 5.0)  ; Defininging these as individual doubles
(def default-r-snipe-prior-1 20.0) ;  rather than a sequence makes them easy to
(def default-k-snipe-prior 10.0)   ;  edit from the UI.
(def default-num-k-snipes 20)
(def default-num-r-snipes default-num-k-snipes)

(cfg/defsimconfig [[initial-snipe-energy double 0.0 10.0]
                   [r-snipe-prior-0      double 1.0 50.0]
                   [r-snipe-prior-1      double 1.0 50.0]
                   [k-snipe-prior        double 1.0 50.0]
                   [num-k-snipes         long   1 200]
                   [num-r-snipes         long   1 200]])

;; produces this (reformatted here):

(do
  (clojure.core/defrecord SimConfigData [initial-snipe-energy r-snipe-prior-0 r-snipe-prior-1 k-snipe-prior num-k-snipes num-r-snipes])

  (clojure.core/gen-class
    :name free-agent.SimConfig
    :state simConfigData
    :exposes-methods {start superStart}
    :init init-sim-config-data
    :main true
    :methods [[getInitialSnipeEnergy [] double]
              [setInitialSnipeEnergy [double] void]
              [getRSnipePrior0 [] double]
              [setRSnipePrior0 [double] void]
              [getRSnipePrior1 [] double]
              [setRSnipePrior1 [double] void]
              [getKSnipePrior [] double]
              [setKSnipePrior [double] void]
              [getNumKSnipes [] long]
              [setNumKSnipes [long] void]
              [getNumRSnipes [] long]
              [setNumRSnipes [long] void]
              [domInitialSnipeEnergy [] java.lang.Object]
              [domRSnipePrior0 [] java.lang.Object]
              [domRSnipePrior1 [] java.lang.Object]
              [domKSnipePrior [] java.lang.Object]
              [domNumKSnipes [] java.lang.Object]
              [domNumRSnipes [] java.lang.Object]])

  (clojure.core/import free-agent.SimConfig)

  (clojure.core/defn -init-sim-config-data
    [seed]
    [[seed] (clojure.core/atom (SimConfigData. default-initial-snipe-energy
                                               default-r-snipe-prior-0
                                               default-r-snipe-prior-1
                                               default-k-snipe-prior
                                               default-num-k-snipes
                                               default-num-r-snipes))])

  (defn -getInitialSnipeEnergy [this] (:initial-snipe-energy @(.simConfigData this)))
  (defn -getRSnipePrior0 [this] (:r-snipe-prior-0 @(.simConfigData this)))
  (defn -getRSnipePrior1 [this] (:r-snipe-prior-1 @(.simConfigData this)))
  (defn -getKSnipePrior [this] (:k-snipe-prior @(.simConfigData this)))
  (defn -getNumKSnipes [this] (:num-k-snipes @(.simConfigData this)))
  (defn -getNumRSnipes [this] (:num-r-snipes @(.simConfigData this)))

  (defn -setInitialSnipeEnergy [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :initial-snipe-energy newval))
  (defn -setRSnipePrior0 [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :r-snipe-prior-0 newval))
  (defn -setRSnipePrior1 [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :r-snipe-prior-1 newval))
  (defn -setKSnipePrior [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :k-snipe-prior newval))
  (defn -setNumKSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-k-snipes newval))
  (defn -setNumRSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-r-snipes newval))

  (defn -domInitialSnipeEnergy [this] (Interval. 0.0 10.0))
  (defn -domRSnipePrior0 [this] (Interval. 1.0 50.0))
  (defn -domRSnipePrior1 [this] (Interval. 1.0 50.0))
  (defn -domKSnipePrior [this] (Interval. 1.0 50.0))
  (defn -domNumKSnipes [this] (Interval. 1 200))
  (defn -domNumRSnipes [this] (Interval. 1 200)))
