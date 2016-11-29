;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

(ns utils.random
  (:import [ec.util MersenneTwisterFast]))
  ;; https://cs.gmu.edu/~sean/research/mersenne/ec/util/MersenneTwisterFast.html

(defn make-long-seed
  [] 
  (- (System/currentTimeMillis)
     (rand-int Integer/MAX_VALUE)))

(defn flush-rng
  "Flush out initial order from a Mersenne Twister."
  [rng]
  (dotimes [_ 1500] (.nextInt rng)))  ; see ;; https://listserv.gmu.edu/cgi-bin/wa?A1=ind1609&L=MASON-INTEREST-L#1

(defn make-rng
  "Make an instance of a MersenneTwisterFast RNG and flush out its initial
  minimal lack of entropy."
  ([] (make-rng (make-long-seed)))
  ([long-seed] 
   (let [rng (MersenneTwisterFast. long-seed)]
     (flush-rng rng)
     rng))) 

(defn rand-idx [rng n] (.nextInt rng n))

(defn next-long [rng] (.nextLong rng))

(defn next-double
"Returns a random double in the half-open range from [0.0,1.0)."
  [rng]
  (.nextDouble rng))

(defn next-gaussian
  "Returns a random double from a Gaussian distribution.  If mean and sd aren't
  supplied, uses a standard Gaussian distribution with mean = 0 and sd = 1,
  otherwise uses the supplied mean and standard deviation."
  ([rng] (.nextGaussian rng))
  ([rng mean sd]
   (+ mean (* sd (next-gaussian rng)))))
