(defproject free-agent "0.1.0-SNAPSHOT"
  :description "Agent-based simulation with free energy minimization within agents."
  :url "https://github.com/mars0i/free-agent"
  :license {:name "Gnu General Public License version 3.0"
            :url "http://www.gnu.org/copyleft/gpl.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [net.mikera/core.matrix "0.57.0"]
                 [net.mikera/vectorz-clj "0.45.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [incanter "1.5.7"]
                 [criterium "0.4.4"]] ; to use, e.g.: (use '[criterium.core :as c])
  :main ^:skip-aot free-agent.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
