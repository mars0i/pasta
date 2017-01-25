(ns free-agent.stats)

(defn get-pop-size
  [cfg-data]
  (count (:snipes (:popenv cfg-data))))

(defn get-k-snipe-freq
  [cfg-data]
  (let [count-k-snipes (fn [n id snipe] (if (sn/k-snipe? snipe) (inc n) n))
        snipes (:snipes (:popenv cfg-data))
        pop-size (count snipes)
        k-snipe-count (reduce-kv count-k-snipes 0 snipes)]
    (if (pos? pop-size)                   ; when UI first starts, it tries to calc this even though there's no pop, and divs by zero
      (double (/ k-snipe-count pop-size)) 
      0))) ; avoid spurious div by zero at beginning of a run


