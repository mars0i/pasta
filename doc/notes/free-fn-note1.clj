;; Notes on what function(s) the free-energy minimization should
;; approximate in snipes

(defn sq 
  "Returns x squared."
  [x] 
  (* x x))

(defn sgn+
  "Return 1 if x >= 0, and -1 otherwise.  (Similar to an sgn function
  but returns 1 rather than 0 when x = 0."
  [x]
  (if (neg? x) -1 1))

;; This function computes a relationship between two mushroom radii
;; and nutritiousness.  The goal is to learn params radius-1,
;; radius-2, and signer s.t. the result is equal to the sign of
;; nutritiousness of the two kinds of mushrooms.
(defn phi [radius-1 radius-2 signer data]
  "Returns 1 or -1 depending on whether data is greater than or less than
  the point intermediate between the squares of the two radii, multiplied by
  signer (which may be either negative or positive)."
  (sgn+ (* signer 
           (- data (/ (+ (sq radius-1) (sq radius-2))
                      2)))))

;; Note though that with this function, what matters is the average
;; of the squared radii, and there are many radii that will produce
;; the same average.  So it's not really necessary to learn the radii.
;; Any two radii with the right average will work.  
;; 
;; So should the parameter just be the average of the squares? 
;; (Note this is not the square of an average.  The expectation of two 
;; squares is not the square of the expectation.  e.g. the average of the 
;; squares of 10 and 12 is 122, but the square of 11 is 121.)
;; 
;; In this model, the function would be:
(defn phi [avg-radius signer data]
  "Returns 1 or -1 depending on whether data is greater than or less than
  the point intermediate between the squares of the two radii, multiplied by
  signer (which may be either negative or positive)."
  (sgn+ (* signer 
           (- data avg-radius))))

;; Note that before applying sgn+, this function is linear.
;; If I'm generating data with a direct Normal dist, rather than
;; squaring it, then what I'm learning is just a function of the 
;; average mushroom area, plus the nutrition sign.




;(defn avg2
;  "Returns the average of two numbers."
;  [x y]
;  (/ (+ x y) 2))
