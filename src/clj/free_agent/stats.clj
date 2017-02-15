(ns free-agent.stats
  (require [free-agent.snipe :as sn]
           [clojure.pprint :as pp]
           [clojure.math.numeric-tower :as math]))

;; from https://clojuredocs.org/clojure.core/reduce-kv#example-57d1e9dae4b0709b524f04eb
(defn map-kv
  "Given a map coll, returns a similar map with the same keys and the result 
  of applying f to each value."
  [f coll]
  (reduce-kv (fn [m k v] (assoc m k (f v)))
             (empty coll) coll))

(defn get-pop-size
  [cfg-data]
  (count (:snipes (:popenv cfg-data))))

;(defn count-snipes
;  [snipes]
;  (let [counter (fn [[k r s] id snipe]
;                  (cond (sn/k-snipe? snipe) [(inc k) r s]
;                        (sn/r-snipe? snipe) [k (inc r) s]
;                        (sn/s-snipe? snipe) [k r (inc s)]
;                        :else (throw (Exception. "bad snipe"))))]
;    (reduce-kv counter [0 0 0] snipes)))

(defn inc-snipe-counts
  "Increments the entry of map counts corresponding to the snipe class."
  [counts s]
  (cond (sn/k-snipe? s)            (update counts :k-snipe inc)
        (sn/s-snipe? s)            (update counts :s-snipe inc)
        (sn/r-snipe-pref-small? s) (update counts :r-snipe-pref-small inc)
        :else                      (update counts :r-snipe-pref-big inc)))

(defn count-snipes
  "Returns a map containing counts for numbers of snipes of the three kinds 
  in snipes.  Keys are named after snipe classes: :k-snipe, 
  :r-snipe-pref-small, :r-snipe-pref-big."
  [snipes]
  (reduce inc-snipe-counts
          {:k-snipe 0, :s-snipe 0, :r-snipe-pref-small 0, :r-snipe-pref-big 0}
          snipes))

(defn count-snipe-locs
  "Given a simple collection of snipes, returns a map containing counts 
  for numbers of snipes of the three kinds in snipes, also classifying 
  r-snipes by the environment (left, right) in which they were found.  
  Keys are named after snipe classes plus left and right for r-snipes: 
  :k-snipe, :r-snipe-pref-small-{left,right}, :r-snipe-pref-big-{left,right}."
  [cfg-data snipes]
  (let [env-center (:env-center cfg-data) ; always = something-and-a-half
        inc-counts (fn [counts s]
                     (cond (sn/k-snipe? s) (update counts :k-snipe inc)
                           (sn/s-snipe? s) (update counts :s-snipe inc)
                           (sn/r-snipe-pref-small? s) (if (< (:x s) env-center)
                                                        (update counts :r-snipe-pref-small-left inc)
                                                        (update counts :r-snipe-pref-small-right inc))
                           (sn/r-snipe-pref-big? s)   (if (< (:x s) env-center)
                                                        (update counts :r-snipe-pref-big-left inc)
                                                        (update counts :r-snipe-pref-big-right inc))))]
    (reduce inc-counts
            {:total (count snipes)
             :k-snipe 0 
             :s-snipe 0 
             :r-snipe-pref-small-left 0
             :r-snipe-pref-small-right 0 
             :r-snipe-pref-big-left 0
             :r-snipe-pref-big-right 0}
            snipes)))

(defn freq-snipe-locs
  [cfg-data snipes]
  (let [counts (count-snipe-locs cfg-data snipes)
        total (:total counts)]
    (map-kv (fn [n] (if (pos? n)
                      (double (/ n total))
                      0))
            (dissoc counts :total))))

(def freqs$ (atom {}))

(defn get-freq
  [cfg-data tick k snipes]
  (let [freqs (or (@freqs$ tick) ; if already got freqs for this tick, use 'em
                  (reset! freqs$  ; if it's a new tick, replace with map containing only new freqs
                         {k (freq-snipe-locs cfg-data snipes)}))] ; i.e. don't keep old freqs
    (k freqs)))

;; OLD
(defn get-k-snipe-freq
  [cfg-data]
  (let [count-k-snipes (fn [n id snipe]
                         (if (sn/k-snipe? snipe)
                           (inc n)
                           n))
        snipes (:snipes (:popenv cfg-data))
        pop-size (count snipes)
        k-snipe-count (reduce-kv count-k-snipes 0 snipes)]
    (if (pos? pop-size)                   ; when UI first starts, it tries to calc this even though there's no pop, and divs by zero
      (double (/ k-snipe-count pop-size)) 
      0))) ; avoid spurious div by zero at beginning of a run
 
(defn count-live-snipe-locs
  [cfg-data]
  (let [snipes (vals (:snipes (:popenv cfg-data)))]
    (count-snipe-locs cfg-data snipes)))

(defn count-dead-snipe-locs
  [cfg-data]
  (let [dead-snipes (:dead-snipes (:popenv cfg-data))]
    (count-snipe-locs cfg-data (apply concat dead-snipes))))

(defn mean-vals-locs
  "Returns a map of mean values for snipe field key k for snipes, with the keys 
  of the new map as in count-snipe-locs. The counts argument should be the result 
  of count-snipe-locs for the same snipes."
  [k cfg-data counts snipes]
  (let [env-center (:env-center cfg-data) ; always = something-and-a-half
        num-snipes (count snipes)
        sum-vals (fn [sums s]
                     (cond (sn/k-snipe? s)            (update sums :k-snipe + (k s))
                           (sn/s-snipe? s)            (update sums :s-snipe + (k s))
                           (sn/r-snipe-pref-small? s) (if (< (:x s) env-center)
                                                        (update sums :r-snipe-pref-small-left + (k s))
                                                        (update sums :r-snipe-pref-small-right + (k s)))
                           (sn/r-snipe-pref-big? s)   (if (< (:x s) env-center)
                                                        (update sums :r-snipe-pref-big-left + (k s))
                                                        (update sums :r-snipe-pref-big-right + (k s)))))
        val-totals (reduce sum-vals 
                           {:k-snipe 0 
                            :s-snipe 0 
                            :r-snipe-pref-small-left 0,
                            :r-snipe-pref-small-right 0 
                            :r-snipe-pref-big-left 0
                            :r-snipe-pref-big-right 0}
                           snipes)]
    (zipmap (sort (keys val-totals)) ; make sure all keys are in same order
            (map #(if (pos? %2) ; don't divide zero by zero
                    (double (/ %1 %2)) ; integer values are close enough, but round returns ugly BigInts
                    nil)
                 (vals (into (sorted-map) val-totals))
                 (vals (into (sorted-map) counts))))))

(defn mean-ages-locs
  "Returns a map of mean ages for snipes, with keys as in count-snipe-locs. The
  counts argument should be the result of count-snipe-locs for the same snipes."
  [cfg-data counts snipes]
  (mean-vals-locs :age cfg-data counts snipes))

(defn mean-ages-live-snipe-locs
  [cfg-data counts]
  (let [snipes (vals (:snipes (:popenv cfg-data)))]
    (mean-ages-locs cfg-data counts snipes)))

(defn mean-ages-dead-snipe-locs
  [cfg-data counts]
  (let [dead-snipes (:dead-snipes (:popenv cfg-data))]
    (mean-ages-locs cfg-data counts (apply concat dead-snipes))))

(defn mean-energies-locs
  "Returns a map of mean energies for snipes, with keys as in count-snipe-locs. The
  counts argument should be the result of count-snipe-locs for the same snipes."
  [cfg-data counts snipes]
  (mean-vals-locs :energy cfg-data counts snipes))

(defn mean-energies-live-snipe-locs
  [cfg-data counts]
  (let [snipes (vals (:snipes (:popenv cfg-data)))]
    (mean-energies-locs cfg-data counts snipes)))

(defn mean-energies-dead-snipe-locs
  [cfg-data counts]
  (let [dead-snipes (:dead-snipes (:popenv cfg-data))]
    (mean-energies-locs cfg-data counts (apply concat dead-snipes))))

(defn mean-prefs-locs
  "Returns a map of mean mush-prefs for snipes, with keys as in count-snipe-locs. The
  counts argument should be the result of count-snipe-locs for the same snipes."
  [cfg-data counts snipes]
  (mean-vals-locs :mush-pref cfg-data counts snipes))

(defn mean-prefs-live-snipe-locs
  [cfg-data counts]
  (let [snipes (vals (:snipes (:popenv cfg-data)))]
    (mean-prefs-locs cfg-data counts snipes)))

(defn round-or-nil
  "Rounds its argument unless the argument is falsey, in which case it's simply
  passed through as is."
  [x]
  (if x
    (math/round x)
    x))

(defn report-stats
  "Report summary statistics to standard output."
  ([cfg-data schedule] 
   (print "At step" (.getSteps schedule) "")
   (report-stats cfg-data))
  ([cfg-data]
   (let [pop-size (get-pop-size cfg-data)
         k-snipe-freq (get-k-snipe-freq cfg-data)
         live-counts (into (sorted-map) (count-live-snipe-locs cfg-data))
         dead-counts (into (sorted-map) (count-dead-snipe-locs cfg-data))
         live-energies (into (sorted-map) (mean-energies-live-snipe-locs cfg-data live-counts))
         live-prefs (into (sorted-map) (mean-prefs-live-snipe-locs cfg-data live-counts))
         ;; no need to caluclate mean dead energies: they're always zero
         live-ages (into (sorted-map) (map-kv round-or-nil (mean-ages-live-snipe-locs cfg-data live-counts)))  ; cl-format ~d directive doesn't round or truncate, etc.
         dead-ages (into (sorted-map) (map-kv round-or-nil (mean-ages-dead-snipe-locs cfg-data dead-counts)))] ; and ages are easier to read as integers
     (pp/cl-format true "pop size: ~d, k-snipe-freq: ~d~%" pop-size k-snipe-freq)
     (pp/cl-format true "live counts ~{~{~a ~d~}~^, ~}~%" live-counts) ; ~{...~} iterates over a sequence; maps treated as sequences become
     (pp/cl-format true "dead counts ~{~{~a ~d~}~^, ~}~%" dead-counts) ;  sequences of pairs; so we embed another ~{...~} to process the pair.
     (pp/cl-format true "mean live energies ~{~{~a ~@{~:[-~;~:*~$~]~}~}~^, ~}~%" live-energies) ; voodoo to print a number with ~$ if non-nil, or "-" otherwise. 
     (pp/cl-format true "mean live prefs ~{~{~a ~@{~:[-~;~:*~$~]~}~}~^, ~}~%" live-prefs)       ;  ...
     (pp/cl-format true "mean live ages ~{~{~a ~@{~:[-~;~:*~d~]~}~}~^, ~}~%" live-ages)         ;  It's needed because I treat an average as nil if no snipes
     (pp/cl-format true "mean dead ages ~{~{~a ~@{~:[-~;~:*~d~]~}~}~^, ~}~%" dead-ages)))) ; also note "~^," emits a comma iff there is more coming

(defn report-params
  "Print parameters in cfg-data to standard output."
  [cfg-data]
  (let [kys (sort (keys cfg-data))]
    (print "Parameters: ")
    (println (map #(str (name %) "=" (% cfg-data)) kys))))
