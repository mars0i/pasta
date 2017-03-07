(defproject pasta "0.2.0-SNAPSHOT"
  :description "Agent-based simulation with free energy minimization within agents."
  :url "https://github.com/mars0i/pasta"
  :license {:name "Gnu General Public License version 3.0"
            :url "http://www.gnu.org/copyleft/gpl.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 ;[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/algo.generic "0.1.2"]
                 ;[incanter "1.5.7"]
                 ;[net.mikera/core.matrix "0.57.0"]
                 ;[net.mikera/vectorz-clj "0.45.0"]
                 ;[criterium "0.4.4"] ; to use, e.g.: (use '[criterium.core :as c])
                 [mason "19"] ; (slightly hacked version) can be installed in local maven repo: uncomment lein-localrepo below, run 'lein localrepo install lib/mason.19.noAddlApps.jar mason 19'
                 ;; Libs that MASON wants and can be gotten from maven.org, so they don't need to be in my lib dir:
                 [javax.media/jmf "2.1.1e"]
                 ;[com.lowagie/itext "1.2"] ; version that comes with MASON. Not in maven.org.
                 [com.lowagie/itext "1.2.3"] ; this is in maven.org
                 [org.jfree/jcommon "1.0.21"]
                 [org.jfree/jfreechart "1.0.17"]
                 [org.beanshell/bsh "2.0b4"]]

   ;:plugins [[lein-localrepo "0.5.3"]
   ;          [lein-expand-resource-paths "0.0.1"]] ; allows wildcards in resource-paths (https://github.com/dchelimsky/lein-expand-resource-paths)
 
   :jvm-opts ["-Xms2g"]
   ;:resource-paths ["lib/*"]
   :source-paths ["src/clj"]
   ;:main pasta.UI
   :aot [pasta.mush pasta.snipe pasta.popenv pasta.SimConfig pasta.UI pasta.core]
   ;:aot [pasta.SimConfig pasta.UI]
   :profiles {:nogui {:main pasta.SimConfig} ; execute this with 'lein with-profile nogui run'
              :gui   {:main pasta.UI}      ; execute this with 'lein with-profile gui run'
              :core  {:main pasta.core}
              :uberjar {:aot :all ;[pasta.snipe pasta.mush pasta.SimConfig pasta.UI]
                        :main pasta.UI
                        ;:main pasta.SimConfig
                        ;:manifest {"Class-Path" ~#(clojure.string/join \space (leiningen.core.classpath/get-classpath %))}
                        }
              }
              
   :target-path "target/%s"
 )


;; simple default version:
;  :main ^:skip-aot pasta.UI
;  :target-path "target/%s"
;  :profiles {:uberjar {:aot :all}})

  ;:java-source-paths ["src/java"]
  ;:main ^:skip-aot pasta.core
  ; jvm-opts ["-Xms1g"]
  ;:jvm-opts ["-Dclojure.compiler.disable-locals-clearing=true"] ; ???: FASTER, and may be useful to debuggers. see https://groups.google.com/forum/#!msg/clojure/8a1FjNvh-ZQ/DzqDz4oKMj0J
  ;:jvm-opts ["-XX:+TieredCompilation" "-XX:TieredStopAtLevel=1"] ; setting this to 1 will produce faster startup but will disable extra optimization of long-running processes
  ;:jvm-opts ["-XX:TieredStopAtLevel=4"] ; more optimization (?)
