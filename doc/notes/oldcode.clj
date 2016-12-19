

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
