
(defn hyphed-to-camel-str
  "Converts a hyphenated string into the corresponding camelcase string."
  [hyphed-str]
  (let [[first-part & other-parts] (s/split hyphed-str #"-")]
    (reduce str 
            first-part 
            (map s/capitalize other-parts))))

(defn ~'set-sim-config-data-from-commandline!
  "Set fields in the SimConfig's simConfigData from parameters passed on the command line."
  [sim-config# cmdline#]
  (let [options# (:options @cmdline#)
        sim-config-data# (.simConfigData sim-config#)]
    (run! #(apply swap! sim-config-data# assoc %) ; arg is a MapEntry, which is sequential? so will function like a list or vector
          options#))
  (reset! commandline nil))

(defn this-subenv-loc-neighbors
  "Returns a MASON sim.util.Bag containing all snipes in the hexagonal region 
  around location <x,y> in its subenv, to a distance of neighbor-radius.  
  This may include the snipe at <x,y>."
  [cfg-data subenv-key x y]
  (subenv-loc-neighbors cfg-data subenv-key x y))

(defn both-subenvs-loc-neighbors
  [cfg-data x y]
  "Returns a MASON sim.util.Bag containing all snipes in the hexagonal region 
  around location <x,y> in both of the subenvs, to a distance of neighbor-radius.  
  This may include the snipe at <x,y>."
  (.addAll (subenv-loc-neighbors cfg-data :west-subenv x y)
           (subenv-loc-neighbors cfg-data :east-subenv x y)))


(defn s-snipe-pref-freq-bias
  "Adopts mushroom preference with a sign like that of its neighbors."
  [rng snipe mush]
  (let [{:keys [x y cfg-data$]} snipe
        {:keys [popenv mush-mid-size neighbor-radius extreme-pref]} @cfg-data$
        {:keys [snipe-field]} popenv
        neighbors (.getHexagonalNeighbors snipe-field x y neighbor-radius Grid2D/TOROIDAL false)
        neighbors-sign (amath/sgn (reduce (fn [sum neighbor]  ; determine whether neighbors with positive or negative prefs
                                            (+ sum (amath/sgn (:mush-pref neighbor)))) ; predominate (or are equal); store sign of result.
                                          0 neighbors)) ; returns zero if no neighbors
        mush-pref (+ (* neighbors-sign extreme-pref) ; -1, 0, or 1 * extreme-pref
                     (pref-noise rng)) ; allows s-snipes to explore preference space even when all snipes are s-snipes
        scaled-appearance (- (mu/appearance mush) mush-mid-size)
        eat? (pos? (* mush-pref scaled-appearance))]  ; eat if scaled appearance has same sign as mush-pref
    [(assoc snipe :mush-pref mush-pref) eat?])) ; mush-pref will just be replaced next time, but this allows inspection

(defn OLD-this-env-neighbors
  [snipe]
  (let [{:keys [x y cfg-data$]} snipe
        {:keys [popenv neighbor-radius]} @cfg-data$
        {:keys [snipe-field]} popenv]
    (.getHexagonalNeighbors snipe-field x y neighbor-radius Grid2D/TOROIDAL false)))

(defn OLD-cross-env-neighbors
  [snipe]
  (let [{:keys [x y cfg-data$]} snipe
        {:keys [popenv neighbor-radius env-width env-center]} @cfg-data$
        {:keys [snipe-field]} popenv
        shifted-x (+ x env-center)
        cross-x (if (< shifted-x env-width)
                  shifted-x
                  (- shifted-x env-width))]
    (concat 
      (.getHexagonalNeighbors snipe-field  ; env for my kind of mushrooms
                              x y
                              neighbor-radius Grid2D/TOROIDAL false)
      (.getHexagonalNeighbors snipe-field  ; env for the other kind of mushrooms
                              cross-x y 
                              neighbor-radius Grid2D/TOROIDAL false))))

(defn s-snipe-pref-success-bias
  "Adopts mushroom preference with a sign like that of its most successful 
  neighbor, where success is measured by current energy.  Ties are broken
  randomly.  (Note this simple measure means that a snipe that's recently 
  given birth and lost the birth cost may appear less successful than one 
  who's e.g. never given birth but is approaching the birth threshold.)"
  [rng snipe mush neighbors]
  (let [{:keys [cfg-data$]} snipe
        {:keys [mush-mid-size]} @cfg-data$
        mush-pref (+ (:mush-pref (best-neighbor rng neighbors))
                     (pref-noise rng)) ; allows s-snipes to explore preference space even when all snipes are s-snipes
        scaled-appearance (- (mu/appearance mush) mush-mid-size)
        eat? (pos? (* mush-pref scaled-appearance))]  ; eat if scaled appearance has same sign as mush-pref
    [(assoc snipe :mush-pref mush-pref) eat?])) ; mush-pref will just be replaced next time, but this allows inspection

(defn s-snipe-pref-success-bias-this-env
  [rng snipe mush]
  (s-snipe-pref-success-bias rng snipe mush (OLD-this-env-neighbors snipe)))

(defn s-snipe-pref-success-bias-cross-env
  [rng snipe mush]
  (s-snipe-pref-success-bias rng snipe mush (OLD-cross-env-neighbors snipe)))

