;; What does `:gen-class` in `ns` do?
;; This is the result of `macroexpand-1` on my 12/6/2016 `ns` for State.clj:

    (do
     (clojure.core/in-ns 'free-agent.State)
     (clojure.core/with-loading-context
    
      (clojure.core/gen-class
       :name "free_agent.State"
       :impl-ns free-agent.State
       :main true
       :name free-agent.State
       :extends sim.engine.SimState
       :exposes-methods {start superStart}
       :methods [[getInitialSnipeEnergy [] double]
                 [setInitialSnipeEnergy [double] void]
                 [getRSnipePriors [] "[D"]
                 [setRSnipePriors ["[D"] void]
                 [setNumKSnipes [long] void]
                 [getNumKSnipes [] long]
                 [setNumRSnipes [long] void]
                 [getNumRSnipes [] long]]
       :state instanceState
       :init init-instance-state
       :main true)
    
      (clojure.core/refer 'clojure.core)
      (clojure.core/require '[clojure.tools.cli])
      (clojure.core/import
       '[sim.engine Steppable Schedule]
       '[ec.util MersenneTwisterFast]
       '[java.lang String]
       '[free-agent State]))
     (if (.equals 'free-agent.State 'clojure.core)
       nil
       (do
        (clojure.core/dosync
         (clojure.core/commute
          @#'clojure.core/*loaded-libs*
    		       clojure.core/conj
    		       'free-agent.State))
    		       nil)))


;; i.e. essentially it's an `in-ns` followed by `gen-class` and
;; then `require`s and `import`s, along with some other framing
;; stuff that I don't understand.  (`macroexpand` gives the same
;; or almost the same result.)
