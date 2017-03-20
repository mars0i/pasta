;; This software is copyright 2016, 2017 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns pasta.stats
  (:require [pasta.snipe :as sn]
            [utils.map2csv :as m2c]
            [clojure.pprint :as pp]
            [clojure.math.numeric-tower :as math]
            [com.rpl.specter :as s]))

;; FIXME contains obsolete fns:
(declare map-kv get-pop-size inc-snipe-counts OLD-count-snipes sum-snipes avg-snipes get-freq maybe-get-freq count-dead-snipe get-k-snipe-freq count-live-snipes mean-vals avg-age avg-energy avg-mush-pref mean-ages mean-ages-live-snipe mean-ages-dead-snipe mean-energies mean-energies-live-snipe mean-energies-dead-snipe mean-prefs mean-prefs-live-snipe round-or-nil report-stats report-params)

;; FIXME There is a lot of obsolete code that needs to be removed.

;; from https://clojuredocs.org/clojure.core/reduce-kv#example-57d1e9dae4b0709b524f04eb
;; Or consider using Specter's (transform MAP-VALS ...)
(defn map-kv
  "Given a map coll, returns a similar map with the same keys and the result 
  of applying f to each value."
  [f coll]
  (reduce-kv (fn [m k v] (assoc m k (f v)))
             (empty coll) coll))

(defn sorted-group-by
  [f coll]
  (into (sorted-map) (group-by f coll)))

(defn sort-map
  [m]
  (into (sorted-map) m))

(defn div-or-zero
  [d n]
  (if (zero? n)
    0
    (/ d n)))

(defn get-pop-size
  [cfg-data]
  (count (:snipe-map (:popenv cfg-data))))

(defn prefix-map-keys
  "Make a map that's like m but that has new keys in whihc string prefix is
  prepended to the name of every key in map m."
  [prefix m]
  (zipmap (map #(keyword (str prefix "-" (name %)))
               (keys m))
          (vals m)))

(defn sum-snipes
  "Given a simple collection (not a map) of snipes, returns a map containing
  sums of values of snipes of different classes.  The sum is due to whatever 
  function f determiness about the snipes.  e.g. with no f argument, we just 
  increment the value to simply count the snipes in each class.  Keys are named after 
  snipe classes: :k-snipe, :r-snipe, :r-snipe, :s-snipe.
  An additional entry, :total, contains a total count of all snipes.  If there
  are additional collection arguments, the counts will be sums from all
  of the collections."
  ([snipes] (sum-snipes snipes (fn [v _] (inc v))))
  ([snipes f]
   (let [summer (fn [sum s]
                      (cond (sn/k-snipe? s) (update sum :k-snipe f s) ; the value of the field in sum will be the first arg to f, followed by s
                            (sn/s-snipe? s) (update sum :s-snipe f s)
                            (sn/r-snipe? s) (update sum :r-snipe f s)))]
     (reduce summer
             {:total (count snipes)
              :k-snipe 0 
              :s-snipe 0 
              :r-snipe 0}
             snipes)))
  ([snipes f & more-snipes]
   (apply merge-with +            ; overhead of map and apply should be minor relative to counting process
          (map #(sum-snipes % f)
               (cons snipes more-snipes))))) ; cons is really cheap here

(defn make-sum-fn
  "Make a function passable to sum-snipes, using key k to extract a field
  from a snipe s in order to add it to the value in the sum map."
  [k]
  (fn [v s]
    (+ v (k s))))

(defn snipe-freqs
  "Given counts that result from sum-snipes, returns a map containing relative 
  frequencies of snipes of different classes, plus the total
  number of snipes examined.  Keys are named after snipe classes: :k-snipe, 
  :r-snipe, :r-snipe-, :s-snipe, plus :total.  Doesn't
  work with quantities other than counts."
  [counts]
  (let [total (:total counts)]
    (if (pos? total)
      (map-kv (fn [n] (double (/ n total))) counts)
      (map-kv (fn [_] 0) counts))))

(defn avg-snipes
  "Computes the average of values in the result of (sum-snipe snipes f).
  That is, divides each value in that result by the number of snipes
  in that class.  If you use the two-argument version, make sure that
  the counts argument comes from the same set of snipes."
  ([snipes f] (avg-snipes snipes f (sum-snipes snipes)))
  ([snipes f counts] (merge-with div-or-zero (sum-snipes snipes f) counts)))

(defn count-dead-snipe
  [cfg-data]
  (let [{:keys [popenv]} cfg-data
        {:keys [west east]} popenv
        west-snipes (apply concat (:dead-snipes west))
        east-snipes (apply concat (:dead-snipes east))]
    (sum-snipes (concat west-snipes east-snipes))))

(defn count-live-snipes
  [cfg-data]
  (let [{:keys [popenv]} cfg-data
        {:keys [west east]} popenv
        snipes (.elements (:snipe-field west))]
    (.addAll snipes (.elements (:snipe-field east)))
    (sum-snipes snipes)))

(defn avg-age
  "Returns a map of mean ages for snipes, with keys as in count-snipes. The
  counts argument should be the result of count-snipes for the same snipes."
  [snipes counts]
  (avg-snipes snipes (make-sum-fn :age) counts))

(defn avg-energy
  "Returns a map of mean ages for snipes, with keys as in count-snipes. The
  counts argument should be the result of count-snipes for the same snipes."
  [snipes counts]
  (avg-snipes snipes (make-sum-fn :energy) counts))

(defn avg-mush-pref
  "Returns a map of mean ages for snipes, with keys as in count-snipes. The
  counts argument should be the result of count-snipes for the same snipes."
  [snipes counts]
  (avg-snipes snipes (make-sum-fn :mush-pref) counts))

(defn round-or-nil
  "Rounds its argument unless the argument is falsey, in which case it's simply
  passed through as is."
  [x]
  (if x
    (math/round x)
    x))

(defn classify-by-snipe-class
  [snipe]
  (cond (sn/k-snipe? snipe) :k
        (sn/r-snipe? snipe) :r
        (sn/s-snipe? snipe) :s
        :else nil))

(defn classify-by-pref
  [snipe]
  (cond (pos? (:mush-pref snipe)) :pos
        (neg? (:mush-pref snipe)) :neg
        :else :zero))

(def group-by-snipe-class (partial sorted-group-by classify-by-snipe-class))
(def group-by-pref (partial sorted-group-by classify-by-pref))
(def group-by-subenv (partial sorted-group-by :subenv))

;(defn group-by-snipe-class [snipes] (into (sorted-map) (group-by classify-by-snipe-class snipes)))
;(defn group-by-pref [snipes] (into (sorted-map) (group-by classify-by-pref snipes)))
;(defn group-by-subenv [snipes] (into (sorted-map) (group-by classify-by-subenv snipes)))

;(def group-by-snipe-class (partial group-by classify-by-snipe-class))
;(def group-by-pref (partial group-by classify-by-pref))
;(def group-by-subenv (partial group-by :subenv))

(defn classify-snipes
  "Returns a hierarchical map of maps of maps of colls of snipes in categories."
  [cfg-data]
   (let [popenv (:popenv cfg-data)
         snipes (concat (.elements (:snipe-field (:west popenv)))
                        (.elements (:snipe-field (:east popenv))))]
     (->> snipes
          (group-by-snipe-class)                                 ; creates a map by snipe class
          (s/transform s/MAP-VALS group-by-subenv)               ; replaces each coll of snipes by a map by subenv
          (s/transform [s/MAP-VALS s/MAP-VALS] group-by-pref)))) ; replaces each coll of snipes by a map by pos/neg mush-pref

(defn sum-by
  [k xs]
  (reduce (fn [sum x] (+ sum (k x)))
          0.0 xs))

(defn subpop-stats
  "Given a collection of snipes, returns a sequence of summary statistics:
  count, average energy, average mush preference, and average age."
  [snipes]
   (let [num-snipes (count snipes)
         avg-energy (/ (sum-by :energy snipes) num-snipes) ; FIXME assumes there are > 0 snipes
         avg-pref (/ (sum-by :mush-pref snipes) num-snipes)
         avg-age (/ (sum-by :age snipes) num-snipes)]
     [num-snipes avg-energy avg-pref avg-age]))
     ;{:count num-snipes :energy avg-energy :pref avg-pref :age avg-age}

(def csv-header ["step" "snipe_class" "subenv" "pref_sign" "count" "energy" "pref" "age"])

;; leaf-seqs
;; Specter navigator operator that allows me to run snipe-stats on a classified snipes 
;; structure that includes a :step element.  I don't follow the ugly Specter convention 
;; of naming navigators with all-caps symbols.  This code based on example under "Recursive 
;; navigation" at http://nathanmarz.com/blog/clojures-missing-piece.html .
;; Note that while there is no primitive test for a non-map collection, because of the way
;; that MAP-VALS works, the code below only tests for coll? when we are no longer looking at 
;; a map; it functions as a test for all non-map collections at that point.
;; (You might think that this function could be replaced by (walker sequential?), but
;; walker walks into maps as if they were sequences, and then sees MapEntrys as sequences.
;; Maybe would work with some more complex predicate instead of sequential?, but then
;; you're doing that multiple-test on every node.  I think that the def I give below
;; is probably better.) 
(def ^{:doc "Specter navigator that recurses into recursively embedded maps of arbitrary 
  depth, operating only on non-map collection leaf values (including sets,
  despite the name of the navigator)."}
  leaf-seqs (s/recursive-path [] p
              (s/if-path map?       ; if you encounter a map
                 [s/MAP-VALS p]     ; then look at all of its vals, and the rest of the structure (i.e. p)
                 [s/STAY coll?])))  ; if it's not a map, but it's a coll, then return it to do stuff with it
                                    ; otherwise, just leave whatever you find there alone

;; note this:
;; (flatten (transform [(recursive-path [] p (if-path map? (continue-then-stay MAP-VALS p)))] first a))

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

;; Based on an answer by miner49r at
;; http://stackoverflow.com/questions/21768802/how-can-i-get-the-nested-keys-of-a-map-in-clojure:
;; See oldcode.clj or commits af85d66 and 340c313 for alternative defs, and 
;; doc/notes/squarestatstimes.txt for speed comparison.  This version and a similar one
;; were about twice as fast as two others on representative stats data trees.
(defn square-stats
  "Given an embedded map structure with sequences of per-category snipe summary
  statistics at the leaves, returns a collection of sequences with string versions
  of the map keys, representing category names, followed by the summary statistics.
  (This prepares the data for writing to a CSV file that can be easily read into
  an R dataframe for use by Lattice graphics.)"
  ([stats] (square-stats [] stats))
  ([prev stats]                ; prev contains keys previously accumulated in one inner sequence
   (reduce-kv (fn [result k v] ; result accumulates the sequence of sequences
                (if (map? v)
                  (into result (square-stats (conj prev (name k)) v)) ; if it's a map, recurse into val, adding key to prev
                  (conj result (concat (conj prev (name k)) v)))) ; otherwise add the most recent key and then add the inner data seq to result
              []    ; outer sequence starts empty
              stats)))

(defn make-stats-at-step
  [cfg-data schedule]
  {:step (.getSteps schedule)
   :stats (snipe-stats (classify-snipes cfg-data))})

(defn square-stats-at-step-for-csv
  [stats-at-step]
  (let [step (:step stats-at-step)
        squared-stats (square-stats (:stats stats-at-step))]
    (map #(cons step %) squared-stats)))

;; Illustration of usage:
;;
;;    user=> (def s804 (square-stats-at-step-for-csv (make-stats-at-step @data$ (.schedule cfg))))
;;    #'user/s804
;;    user=> (def s911 (square-stats-at-step-for-csv (make-stats-at-step @data$ (.schedule cfg))))
;;    #'user/s911
;;    user=> (pprint (cons csv-header (concat s804 s911)))
;;    (["step" "snipe_class" "subenv" "pref_sign" "count" "energy" "pref" "age"]
;;     (804 "k" "east" "pos" 23 11.26086956521739 2.7933525400119586E-4 600.0869565217391)
;;     (804 "k" "west" "neg" 39 13.35897435897436 -2.194019839094257E-4 581.9230769230769)
;;     (804 "r" "east" "neg" 25 7.24 -1.0 263.48)
;;     (804 "r" "east" "pos" 35 14.714285714285714 1.0 444.34285714285716)
;;     (804 "r" "west" "neg" 37 15.08108108108108 -1.0 466.8918918918919)
;;     (804 "r" "west" "pos" 24 7.333333333333333 1.0 523.625)
;;     (804 "s" "east" "neg" 20 6.55 -0.650021630731745 345.55)
;;     (804 "s" "east" "pos" 21 13.904761904761905 0.9048001299409795 484.57142857142856)
;;     (804 "s" "east" "zero" 1 10.0 0.0 4.0)
;;     (804 "s" "west" "neg" 47 14.297872340425531 -0.7447054437863198 451.72340425531917)
;;     (804 "s" "west" "pos" 12 7.583333333333333 0.916669501580321 316.1666666666667)
;;     (804 "s" "west" "zero" 1 10.0 0.0 14.0)
;;     (911 "k" "east" "pos" 28 10.714285714285714 2.7141106574043447E-4 593.1428571428571)
;;     (911 "k" "west" "neg" 41 13.121951219512194 -2.481524592827564E-4 648.3658536585366)
;;     (911 "k" "west" "pos" 2 12.5 5.415420603792033E-6 218.0)
;;     (911 "k" "west" "zero" 1 10.0 0.0 31.0)
;;     (911 "r" "east" "neg" 31 6.967741935483871 -1.0 230.32258064516128)
;;     (911 "r" "east" "pos" 39 15.256410256410257 1.0 500.79487179487177)
;;     (911 "r" "west" "neg" 46 14.826086956521738 -1.0 471.3695652173913)
;;     (911 "r" "west" "pos" 25 7.12 1.0 487.68)
;;     (911 "s" "east" "neg" 24 6.416666666666667 -0.6667013107370153 356.9583333333333)
;;     (911 "s" "east" "pos" 26 14.615384615384615 0.8846628654367455 487.7307692307692)
;;     (911 "s" "east" "zero" 1 10.0 0.0 6.0)
;;     (911 "s" "west" "neg" 52 14.576923076923077 -0.7692529972684045 511.7307692307692)
;;     (911 "s" "west" "pos" 15 7.266666666666667 0.866699845811923 288.53333333333336)
;;     (911 "s" "west" "zero" 3 10.0 0.0 9.0))

;; TODO rewrite using new data collection functions
(defn write-stats-to-console
  "Report summary statistics to standard output."
  ([cfg-data schedule] 
   (print "At step" (.getSteps schedule) "")
   (report-stats cfg-data))
  ([cfg-data]
   (let [popenv (:popenv cfg-data)
         pop-size (get-pop-size cfg-data)
         west-snipes (.elements (:snipe-field (:west popenv)))
         east-snipes (.elements (:snipe-field (:east popenv)))
         snipes (concat west-snipes east-snipes)
         west-counts (sort-map (sum-snipes west-snipes))
         east-counts (sort-map (sum-snipes east-snipes))
         counts (sort-map (merge-with + west-counts east-counts))
         freqs (sort-map (snipe-freqs counts))
         west-prefs (sort-map (avg-mush-pref west-snipes west-counts))
         east-prefs (sort-map (avg-mush-pref east-snipes east-counts))
         energies (sort-map (avg-energy snipes counts))
         ages (sort-map (map-kv round-or-nil (avg-age snipes counts)))]
         ;; dead-counts (into (sorted-map) (count-dead-snipe cfg-data)) FIXME
         ;dead-ages (into (sorted-map) (map-kv round-or-nil (mean-ages-dead-snipe cfg-data dead-counts))) FIXME ; and ages are easier to read as integers

     ;; ~{...~} iterates over a sequence; maps treated as sequences become sequences of pairs; 
     ;; so we embed another ~{...~} to process the pair.  also note "~^," emits a comma iff there is more coming.
     (pp/cl-format true "counts ~{~{~a ~d~}~^, ~}~%" counts)
     (pp/cl-format true "freqs ~{~{~a ~3$~}~^, ~}~%" freqs)
     (pp/cl-format true "mean west-prefs ~{~{~a ~@{~:[-~;~:*~5$~]~}~}~^, ~}~%" west-prefs)       ;  ...
     (pp/cl-format true "mean east-prefs ~{~{~a ~@{~:[-~;~:*~5$~]~}~}~^, ~}~%" east-prefs)       ;  ...
     (pp/cl-format true "mean energies ~{~{~a ~@{~:[-~;~:*~$~]~}~}~^, ~}~%" energies) ; voodoo to print a number with ~$ if non-nil, or "-" otherwise. 
     (pp/cl-format true "mean ages ~{~{~a ~@{~:[-~;~:*~d~]~}~}~^, ~}~%" ages)         ;  It's needed because I treat an average as nil if no snipes
     ;(pp/cl-format true "dead counts ~{~{~a ~d~}~^, ~}~%" dead-counts) ; 
     ;(pp/cl-format true "mean dead ages ~{~{~a ~@{~:[-~;~:*~d~]~}~}~^, ~}~%" dead-ages)
     )))

;; TODO add conditioning on :write-csv
(defn report-stats
  ([cfg-data schedule] (write-stats-to-console cfg-data schedule))
  ([cfg-data]          (write-stats-to-console cfg-data)))

(defn write-params-to-console
  "Print parameters in cfg-data to standard output."
  [cfg-data]
  (let [kys (sort (keys cfg-data))]
    (print "Parameters: ")
    (println (map #(str (name %) "=" (% cfg-data)) kys))))

(defn write-params-to-file
  ([cfg-data] (write-params-to-file cfg-data 
                                    (str (:csv-basename cfg-data) 
                                         "Params" 
                                         (:seed cfg-data)
                                         ".csv")))
  ([cfg-data f] (m2c/spit-map f cfg-data)))

(defn report-params
  [cfg-data]
  (if (:write-csv cfg-data)
    (write-params-to-file cfg-data)
    (write-params-to-console cfg-data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; USED BY GUI INSPECTORS DEFINED IN pasta.simconfig

(def freqs$ (atom {}))

(defn get-freq
  "Given an integer tick representing a MASON step, and a key k
  for a snipes class (:k-snipe, :r-snipe, :r-snipe,
  :s-snipe) or :total, returns the relative frequency of that snipe class
  in the current population in popenv, or the total number of snipes if
  :total is passed.  Note that data from previous ticks isn't kept.
  tick is just used to determine whether the requested data is from the
  same timestep as the last time that get-freq was called.  If not, then
  all of the frequencies are recalculated from the current population,
  and are associated with the newly passed tick, whether it's actually the 
  current tick or not."
  [tick k popenv]
  (let [freqs (or (@freqs$ tick) ; if already got freqs for this tick, use 'em; else make 'em:
                  (let [{:keys [west east]} popenv
                        snipes (.elements (:snipe-field west))
                        _ (.addAll snipes (.elements (:snipe-field east)))
                        new-freqs (snipe-freqs (sum-snipes snipes))]
                    (reset! freqs$ {tick new-freqs})
                    new-freqs))]
    (k freqs)))

(defn maybe-get-freq
  "Kludge: Calls get-freq if and only if at timestep 1 or later.  Avoids
  irrelevant NPEs during initial setup."
  [tick k popenv]
  (if (and tick (pos? tick))
    (get-freq tick k popenv)
    0.0))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; OLD DEPRECATED/OBSOLETE CODE (maybe still in use)

;; OBSOLETE
;; (defn get-k-snipe-freq
;;   [cfg-data]
;;   (let [count-k-snipes (fn [n id snipe]
;;                          (if (sn/k-snipe? snipe)
;;                            (inc n)
;;                            n))
;;         snipes (:snipe-map (:popenv cfg-data))
;;         pop-size (count snipes)
;;         k-snipe-count (reduce-kv count-k-snipes 0 snipes)]
;;     (if (pos? pop-size)                   ; when UI first starts, it tries to calc this even though there's no pop, and divs by zero
;;       (double (/ k-snipe-count pop-size)) 
;;       0))) ; avoid spurious div by zero at beginning of a run

;; DEPRECATED
;; (defn inc-snipe-counts
;;   "Increments the entry of map counts corresponding to the snipe class."
;;   [counts s]
;;   (cond (sn/k-snipe? s) (update counts :k-snipe inc)
;;         (sn/s-snipe? s) (update counts :s-snipe inc)
;;         :else           (update counts :r-snipe inc)))

;; DEPRECATED
;; (defn OLD-count-snipes
;;   "Returns a map containing counts for numbers of snipes of the three kinds 
;;   in snipes.  Keys are named after snipe classes: :k-snipe, 
;;   :r-snipe, :r-snipe."
;;   [snipes]
;;   (reduce inc-snipe-counts
;;           {:k-snipe 0, :s-snipe 0, :r-snipe 0}
;;           snipes))

;; TODO OLD, BROKEN
;; (defn mean-vals
;;   "Returns a map of mean values for snipe field key k for snipes, with the keys 
;;   of the new map as in count-snipes. The counts argument should be the result 
;;   of count-snipes for the same snipes."
;;   [k cfg-data counts snipes]
;;   (let [env-center (:env-center cfg-data) ; always = something-and-a-half
;;         num-snipes (count snipes)
;;         sum-vals (fn [sums s]
;;                      (cond (sn/k-snipe? s)            (update sums :k-snipe + (k s))
;;                            (sn/s-snipe? s)            (update sums :s-snipe + (k s))
;;                            (sn/r-snipe-pref-small? s) (if (< (:x s) env-center)
;;                                                         (update sums :r-snipe-pref-small-left + (k s))
;;                                                         (update sums :r-snipe-pref-small-right + (k s)))
;;                            (sn/r-snipe-pref-big? s)   (if (< (:x s) env-center)
;;                                                         (update sums :r-snipe-pref-big-left + (k s))
;;                                                         (update sums :r-snipe-pref-big-right + (k s)))))
;;         val-totals (reduce sum-vals 
;;                            {:k-snipe 0 
;;                             :s-snipe 0 
;;                             :r-snipe-pref-small-left 0,
;;                             :r-snipe-pref-small-right 0 
;;                             :r-snipe-pref-big-left 0
;;                             :r-snipe-pref-big-right 0}
;;                            snipes)]
;;     (zipmap (sort (keys val-totals)) ; make sure all keys are in same order
;;             (map #(if (pos? %2) ; don't divide zero by zero
;;                     (double (/ %1 %2)) ; integer values are close enough, but round returns ugly BigInts
;;                     nil)
;;                  (vals (into (sorted-map) val-totals))
;;                  (vals (into (sorted-map) counts))))))
;; 
;; ;; TODO OLD, BROKEN
;; (defn mean-ages
;;   "Returns a map of mean ages for snipes, with keys as in count-snipes. The
;;   counts argument should be the result of count-snipes for the same snipes."
;;   [cfg-data counts snipes]
;;   (mean-vals :age cfg-data counts snipes))
;; 
;; ;; TODO OLD, BROKEN
;; (defn mean-ages-live-snipe
;;   [cfg-data counts]
;;   (let [snipes (vals (:snipes (:popenv cfg-data)))]
;;     (mean-ages cfg-data counts snipes)))
;; 
;; ;; TODO OLD, BROKEN
;; (defn mean-ages-dead-snipe
;;   [cfg-data counts]
;;   (let [dead-snipes (:dead-snipes (:popenv cfg-data))]
;;     (mean-ages cfg-data counts (apply concat dead-snipes))))
;; 
;; ;; TODO OLD, BROKEN
;; (defn mean-energies
;;   "Returns a map of mean energies for snipes, with keys as in count-snipes. The
;;   counts argument should be the result of count-snipes for the same snipes."
;;   [cfg-data counts snipes]
;;   (mean-vals :energy cfg-data counts snipes))
;; 
;; ;; TODO OLD, BROKEN
;; (defn mean-energies-live-snipe
;;   [cfg-data counts]
;;   (let [snipes (vals (:snipes (:popenv cfg-data)))]
;;     (mean-energies cfg-data counts snipes)))
;; 
;; ;; TODO OLD, BROKEN
;; (defn mean-energies-dead-snipe
;;   [cfg-data counts]
;;   (let [dead-snipes (:dead-snipes (:popenv cfg-data))]
;;     (mean-energies cfg-data counts (apply concat dead-snipes))))
;; 
;; ;; TODO OLD, BROKEN
;; (defn mean-prefs
;;   "Returns a map of mean mush-prefs for snipes, with keys as in count-snipes. The
;;   counts argument should be the result of count-snipes for the same snipes."
;;   [cfg-data counts snipes]
;;   (mean-vals :mush-pref cfg-data counts snipes))
;; 
;; ;; TODO OLD, BROKEN
;; (defn mean-prefs-live-snipe
;;   [cfg-data counts]
;;   (let [snipes (vals (:snipes (:popenv cfg-data)))]
;;     (mean-prefs cfg-data counts snipes)))
