;; This software is copyright 2016 by Marshall Abrams, and is distributed
;; under the Gnu General Public License version 3.0 as specified in the
;; the file LICENSE.

;; matrix arithmetic macros
;; there's another free.arithmetic with scalar operations

;; Note that for Clojurescript, this is a macro namespace, which means that it only runs
;; in Clojure, before Clojurescript compilation.  So there's no reason to use any reader macros
;; except to prevent something from happening during Clojurescript pre-compilation.

(ns free-agent.arithmetic
  (:require [clojure.core.matrix :as mx]))
;; *NOTE*: Any file that require-macro's this namespace in Clojurescript will need 
;; to require core.matrix, since the macros below will expand into functions containing 
;; literal core.matrix calls before the Clojurescript compiler sees the code.

(mx/set-current-implementation :vectorz)

;; List of all namespaces of implementations in KNOWN-IMPLEMENTATIONS in
;; https://github.com/mikera/core.matrix/blob/develop/src/main/clojure/clojure/core/matrix/implementations.cljc

;; Clojurescript options:
;; [thinktopic.aljabr.core :as imp]
;; NOT?: [clojure.core.matrix.impl.ndarray-object :as imp] ;; (why did I think this worked in Clojurescript?)

;; Clojure options:
;; (mx/set-current-implementation :ndarray)
;; (mx/set-current-implementation :aljabr)
;; (mx/set-current-implementation :vectorz)
;; (mx/set-current-implementation :clatrix)
;; (mx/set-current-implementation :nd4clj)

;; DON'T do this:
;(println "Loading core.matrix operators.  Matrix implementation:" (mx/current-implementation))
;; In Clojurescript, it will get inserted into a .js file raw and
;; break things in mysterious ways.

;; Note that these are functions, but in free.scalar-arithmetic, I define 
;; them as macros for the sake of performance.  So don't e.g. map the functions 
;; below over a sequence, if you want to preserve substitutability with their 
;; scalar analogues.
;(def m* mx/mmul) ; matrix multiplication and inner product
;(def e* mx/mul)  ; elementwise (Hadamard) and scalar multiplication
;(def m+ mx/add)  ; elementwise addition
;(def m- mx/sub)  ; elementwise subtraction, or elementwise negation
;(def tr mx/transpose)
;(def inv mx/inverse)
;(def make-identity-obj mx/identity-matrix)
;(def pm mx/pm)

(def scalar-sigma-min 1.0)

(defmacro m* 
  "Matrix multiplication."
  ([x y] `(mx/mmul ~x ~y))
  ([x y z] `(mx/mmul ~x ~y ~z)))

(defmacro e* 
  "Elementwise (Hadamard) multiplication of matrices."
  ([x y] `(mx/mul ~x ~y))
  ([x y z] `(mx/mul ~x ~y ~z)))

(defmacro m+ 
  "Elementwise addition, i.e. matrix addition."
  ([x y] `(mx/add ~x ~y))
  ([x y z] `(mx/add ~x ~y ~z)))

(defmacro m- 
  "Elementwise subtraction, i.e. matrix subtraction."
  ([x] `(mx/sub ~x))
  ([x y] `(mx/sub ~x ~y))
  ([x y z] `(mx/sub ~x ~y ~z)))

(defmacro tr
  "Matrix transposition; returns the argument unchanged."
  [x]
  `(mx/transpose ~x))

;; aljabr has neither inverse nor determinant as of Nov 2016.
;; Nor does core.matrix have Clojure versions of these routines;
;; they default to using vectorz, which is Java so isn't available
;; in Clojurescript.
;; re implementing inverse, these seem especially helpful:
;; http://www.math.nyu.edu/~neylon/linalgfall04/project1/jja/group7.htm
;; www.caam.rice.edu/~yzhang/caam335/F09/handouts/lu.pdf
;; also cf https://github.com/mikera/core.matrix/issues/38

(defmacro mat-reciprocal
  "Returns the reciprocal of number x wrapped in a 1x1 matrix."
  [x]
  `(mx/matrix [[(/ 1 ~x)]]) )

(defmacro inv22
  "Simplistic compultation of inverse of a 2x2 matrix.  (Note that due to
  floating point imprecision, multiplying the result by the original might 
  produce an 'identity' matrix only very close to what it should be.)"
  [m] 
  `(let [m00# (mx/mget ~m 0 0)
         m01# (mx/mget ~m 0 1)
         m10# (mx/mget ~m 1 0)
         m11# (mx/mget ~m 1 1)
         adj# (mx/matrix [[m11#  (- m01#)]     ; adjoint
                          [(- m10#) m00#]])
         det# (- (* m00# m11#) (* m01# m10#))] ; determinant
     (mx/div adj# det#)))

;; Since Clojurescript uses Clojure to compile macros, we can't use reader 
;; conditionals to choose between macro definitions.  Instead I use this env
;; trick from https://github.com/tonsky/datascript/blob/master/src/datascript/arrays.cljc.
;; A blog post on it: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing
(defmacro inv
  "Matrix inverse."
  [m]
  (if (:ns &env)  ; if in Clojurescript's Clojure pre-processing stage
    `(case (mx/shape ~m) ; Clojurescript version of macro def
       nil   (/ ~m)
       [1]   (mat-reciprocal ~m) ; core.matrix returns the same
       [[1]] (mat-reciprocal ~m) ; result for both of these cases.
       [2 2] (inv22 ~m)
       (throw (js/Error. (str "Clojurescript matrix inversion not implemented for " ~m))))
    `(mx/inverse ~m))) ; Clojure version

(defmacro make-identity-obj
  "Returns an identity matrix with dims rows."
  [dims]
  `(mx/identity-matrix ~dims))

(defmacro mat-max
  "Returns the max of numbers x and y wrapped in a 1x1 matrix."
  [x y]
  `(mx/matrix [[(max ~x ~y)]]))

;; see Bogacz end of sect 2.4
;; make it a macro simply because the others are (hack for Clojurescript)
;; For matrices, should use 'positive-definite?'?, which is not yet implemented 
;; in core.matrix or maybe test for determinant being > 0 or some larger number?
(defmacro limit-sigma
  "If sigma is a scalar variance or a single-element vector or matrix, then clip the
  value to be no less than sigma-min.  If sigma is a covariance matrix of at least 
  2x2 size, just return it as is, because I'm not yet sure how to limit it.  
  (Using determinant > n? positive definite? Neither's widely implemented in core.matrix.)"
  [sigma]
  `(case (mx/shape ~sigma)
       nil   (max     ~sigma ~scalar-sigma-min)
       [1]   (mat-max ~sigma ~scalar-sigma-min)
       [[1]] (mat-max ~sigma ~scalar-sigma-min)
     ~sigma))

;; btw There's also an e* in core.matrix (which is *almost* identical to mul)
;; but by qualifying core.matrix with mx, it doesn't matter.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; These have no (and need no) equivalents in scalar-arithmetic:
;; Macros only for the sake of Clojurescript.  Maybe move to a different
;; file later.

(defmacro col-mat
  "Turns a sequence of numbers xs into a column vector."
  [xs]
  `(mx/matrix (map vector ~xs)))

(defmacro row-mat
  "Turns a sequence of numbers xs into a row vector."
  [xs]
  `(mx/matrix (vector ~xs)))
