(ns free-agent.stats
  (require [free-agent.snipe :as sn]))

(defn get-pop-size
  [cfg-data]
  (count (:snipes (:popenv cfg-data))))

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

(defn inc-snipe-counts
  "Increments the entry of map counts corresponding to the snipe class."
  [counts s]
  (cond (sn/k-snipe? s)            (update counts :k-snipe inc)
        (sn/r-snipe-pref-small? s) (update counts :r-snipe-pref-small inc)
        :else                      (update counts :r-snipe-pref-big inc)))

(defn count-snipes
  "Returns a map containing counts for numbers of snipes of the three kinds 
  in snipes.  Keys are named after snipe classes: :k-snipe, 
  :r-snipe-pref-small, :r-snipe-pref-big."
  [snipes]
  (reduce inc-snipe-counts
          {:k-snipe 0, :r-snipe-pref-small 0, :r-snipe-pref-big 0}
          snipes))

(defn count-snipe-locs
  "Returns a map containing counts for numbers of snipes of the three kinds 
  in snipes, also classifying r-snipes by the environment (left, right) in
  which they were found.  Keys are named after snipe classes plus left and 
  right for r-snipes: :k-snipe, :r-snipe-pref-small-{left,right},
  :r-snipe-pref-big-{left,right}."
  [cfg-data snipes]
  (let [env-center (:env-center cfg-data) ; always = something-and-a-half
        inc-counts (fn [counts s]
                     (cond (sn/k-snipe? s) (update counts :k-snipe inc)
                           (sn/r-snipe-pref-small? s) (if (< (:x s) env-center)
                                                        (update counts :r-snipe-pref-small-left inc)
                                                        (update counts :r-snipe-pref-small-right inc))
                           (sn/r-snipe-pref-big? s)   (if (< (:x s) env-center)
                                                        (update counts :r-snipe-pref-big-left inc)
                                                        (update counts :r-snipe-pref-big-right inc))))]
    (reduce inc-counts
            {:k-snipe 0 
             :r-snipe-pref-small-left 0,
             :r-snipe-pref-small-right 0 
             :r-snipe-pref-big-left 0
             :r-snipe-pref-big-right 0}
            snipes)))

 
(defn count-live-snipe-locs
  [cfg-data]
  (let [snipes (vals (:snipes (:popenv cfg-data)))]
    (count-snipe-locs cfg-data snipes)))

(defn count-dead-snipe-locs
  [cfg-data]
  (let [dead-snipes (:dead-snipes (:popenv cfg-data))]
    (count-snipe-locs cfg-data (apply concat dead-snipes))))
