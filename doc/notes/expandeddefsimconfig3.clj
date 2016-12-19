

 (do
   (clojure.core/ns free-agent.config-data)
   (clojure.core/defrecord SimConfigData [world-width world-height initial-energy k-snipe-prior r-snipe-prior-0 r-snipe-prior-1 num-k-snipes num-r-snipes mushroom-prob mushroom-mean-0 mushroom-mean-1 mushroom-sd])
   (clojure.core/ns free-agent.SimConfig
     (:require [free-agent.config-data])
     (:import free-agent.SimConfig)
     (:gen-class
       :name free-agent.SimConfig
       :extends sim.engine.SimState
       :state simConfigData
       :exposes-methods {start superStart}
       :init init-sim-config-data
       :main true
       :methods [[getWorldWidth [] double]
                 [setWorldWidth [double] void]
                 [getWorldHeight [] double]
                 [setWorldHeight [double] void]
                 [getInitialEnergy [] double]
                 [setInitialEnergy [double] void]
                 [getKSnipePrior [] double]
                 [setKSnipePrior [double] void]
                 [getRSnipePrior0 [] double]
                 [setRSnipePrior0 [double] void]
                 [getRSnipePrior1 [] double]
                 [setRSnipePrior1 [double] void]
                 [getNumKSnipes [] long]
                 [setNumKSnipes [long] void]
                 [getNumRSnipes [] long]
                 [setNumRSnipes [long] void]
                 [getMushroomProb [] double]
                 [setMushroomProb [double] void]
                 [getMushroomMean0 [] double]
                 [setMushroomMean0 [double] void]
                 [getMushroomMean1 [] double]
                 [setMushroomMean1 [double] void]
                 [getMushroomSd [] double]
                 [setMushroomSd [double] void]
                 [domInitialEnergy [] java.lang.Object]
                 [domKSnipePrior [] java.lang.Object]
                 [domRSnipePrior0 [] java.lang.Object]
                 [domRSnipePrior1 [] java.lang.Object]
                 [domNumKSnipes [] java.lang.Object]
                 [domNumRSnipes [] java.lang.Object]
                 [domMushroomProb [] java.lang.Object]]))
   (clojure.core/defn -init-sim-config-data [seed] [[seed] (clojure.core/atom (free-agent.config-data/->SimConfigData 200 200 10.0 10.0 5.0 20.0 20 20 0.1 4.0 16.0 2.0))])
   (defn -getWorldWidth [this] (:world-width @(.simConfigData this)))
   (defn -getWorldHeight [this] (:world-height @(.simConfigData this)))
   (defn -getInitialEnergy [this] (:initial-energy @(.simConfigData this)))
   (defn -getKSnipePrior [this] (:k-snipe-prior @(.simConfigData this)))
   (defn -getRSnipePrior0 [this] (:r-snipe-prior-0 @(.simConfigData this)))
   (defn -getRSnipePrior1 [this] (:r-snipe-prior-1 @(.simConfigData this)))
   (defn -getNumKSnipes [this] (:num-k-snipes @(.simConfigData this)))
   (defn -getNumRSnipes [this] (:num-r-snipes @(.simConfigData this)))
   (defn -getMushroomProb [this] (:mushroom-prob @(.simConfigData this)))
   (defn -getMushroomMean0 [this] (:mushroom-mean-0 @(.simConfigData this)))
   (defn -getMushroomMean1 [this] (:mushroom-mean-1 @(.simConfigData this)))
   (defn -getMushroomSd [this] (:mushroom-sd @(.simConfigData this)))
   (defn -setWorldWidth [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :world-width newval))
   (defn -setWorldHeight [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :world-height newval))
   (defn -setInitialEnergy [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :initial-energy newval))
   (defn -setKSnipePrior [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :k-snipe-prior newval))
   (defn -setRSnipePrior0 [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :r-snipe-prior-0 newval))
   (defn -setRSnipePrior1 [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :r-snipe-prior-1 newval))
   (defn -setNumKSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-k-snipes newval))
   (defn -setNumRSnipes [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :num-r-snipes newval))
   (defn -setMushroomProb [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mushroom-prob newval))
   (defn -setMushroomMean0 [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mushroom-mean-0 newval))
   (defn -setMushroomMean1 [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mushroom-mean-1 newval))
   (defn -setMushroomSd [this newval] (clojure.core/swap! (.simConfigData this) clojure.core/assoc :mushroom-sd newval))
   (defn -domInitialEnergy [this] (Interval. 0.0 20.0))
   (defn -domKSnipePrior [this] (Interval. 1.0 50.0))
   (defn -domRSnipePrior0 [this] (Interval. 1.0 50.0))
   (defn -domRSnipePrior1 [this] (Interval. 1.0 50.0))
   (defn -domNumKSnipes [this] (Interval. 1 200))
   (defn -domNumRSnipes [this] (Interval. 1 200))
   (defn -domMushroomProb [this] (Interval. 0.0 1.0)))
