
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

(defn snipe-stats
  "Given a hierarchy of maps produced by classify-snipes (optionally
  with extra map entries such as one listing the step at which the
  data was collected), returns a map with the same structure but
  with leaf snipe collections replaced by maps of summary statistics
  produced by subpop-stats."
  [classified-snipes]
  (s/transform [leaf-seqs]  ; or e.g. [s/MAP-VALS s/MAP-VALS], but that's restricted to exactly two levels
                subpop-stats
                classified-snipes))

;; Based on answers by amalloy at
;; http://stackoverflow.com/questions/21768802/how-can-i-get-the-nested-keys-of-a-map-in-clojure:
;; and noisesmith's at
;; http://stackoverflow.com/questions/25268818/get-key-chains-of-a-tree-in-clojure
(defn square-stats
  "Given an embedded map structure with sequences of per-category snipe summary
  statistics at the leaves, returns a collection of sequences with string versions
  of the map keys, representing category names, followed by the summary statistics.
  (This prepares the data for writing to a CSV file that can be easily read into
  an R dataframe for use by Lattice graphics.)"
  [stats]
  (cond (map? stats) (for [[k v] stats           ; for every MapEntry
                           ks (square-stats v)] ; and every subsidiary seq returned
                       (cons (name k) ks))       ; add key's name to each seq returned
        :else [stats])) ; start with data from vectors in innermost vals

;        (sequential? stats) [stats] ; start with data from vectors in innermost vals
;        :else (throw 
;                (Exception. (str "stats structure has an unexpected component: " stats)))))


;; Based on answers by miner49r at
;; http://stackoverflow.com/questions/21768802/how-can-i-get-the-nested-keys-of-a-map-in-clojure:
(defn square-stats*
  ([m] (square-stats* [] m))
  ([prev m]                   ; prev is the keys previously accumulated in one inner sequence
   (reduce-kv (fn [res k v]   ; res accumulates the sequence of sequences
                (if (map? v)
                  (into res (square-stats* (conj prev (name k)) v)) ; if it's a map, recurse into val, adding key to prev
                  (conj res (concat (conj prev (name k)) v)))) ; otherwise add the most recent key and then add the inner seq to res
              []    ; outer sequence starts empty
              m)))

;; Based on answers by miner49r at
;; http://stackoverflow.com/questions/21768802/how-can-i-get-the-nested-keys-of-a-map-in-clojure:
(defn square-stats**
  "Given an embedded map structure with sequences of per-category snipe summary
  statistics at the leaves, returns a collection of sequences with string versions
  of the map keys, representing category names, followed by the summary statistics.
  (This prepares the data for writing to a CSV file that can be easily read into
  an R dataframe for use by Lattice graphics.)"
  ([m] (square-stats* [] m))
  ([prev m]                   ; prev is the keys previously accumulated in one inner sequence
   (reduce-kv (fn [res k v]   ; res accumulates the sequence of sequences
                (if (map? v)
                  (into res (square-stats** (conj prev (name k)) v)) ; if it's a map, recurse into val, adding key to prev
                  (conj res (reduce conj prev (cons (name k) v))))) ; otherwise add the most recent key and summary stats, then add the inner seq to res
              []    ; outer sequence starts empty
              m)))

;; Specter version based on version by Nathan Marz at
;; https://clojurians.slack.com/archives/C0FVDQLQ5/p1489779215484550
;; CAN I GET RID OF THE CONCAT AND ALL AT END?
;; cf. Nathan Marz's other version in keypaths.clj
;; Nathan Marz says: @mars0i for that it's best to just fix it after the selection, with regular clojure or a transform call
;; it's possible to do it in one path with specter's zipper integration, but it won't be particularly elegant
;; See https://clojurians.slack.com/archives/C0FVDQLQ5/p1489970585037139
(defn square-stats***
  "Given an embedded map structure with sequences of per-category snipe summary
  statistics at the leaves, returns a collection of sequences with string versions
  of the map keys, representing category names, followed by the summary statistics.
  (This prepares the data for writing to a CSV file that can be easily read into
  an R dataframe for use by Lattice graphics.)"
  [stats]
  (let [not-quite-flat (s/select (s/recursive-path [] p
                                    (s/if-path map?
                                       [s/ALL                                 ; for each MapEntry
                                        (s/collect-one s/FIRST (s/view name)) ; add the name of its key to
                                        s/LAST p]                             ; passing its val to <recurse>
                                       s/STAY)) ; return what we've got (a val from a map), and stop this branch
                                  stats)]
    (map #(concat (butlast %)
                  (last %))
         not-quite-flat)))
