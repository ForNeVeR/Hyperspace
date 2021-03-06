(ns hyperspace.library.geometry
  (:use clojure.tools.logging))

(defn vector-sum
  "Returns the sum of vectors."
  [& vectors]
  (apply (partial mapv +) vectors))

(def
  ^{:arglists '([vtr & vtrs])
    :doc "If  vtrs are supplied, returns the negation of vtr, else
  subtracts the vtrs from vtr and returns the result."}
  vector-subtract
  (partial mapv -))

(defn multiply-by-scalar
  "Returns the multiplication of vtr and scalar."
  [vtr scalar]
  (mapv #(* scalar %) vtr))

(defn polar->cartesian
  "Converts polar coordinates to the cartesian ones."
  [[a, d]]
  [(* d (Math/cos a))
   (* d (Math/sin a))])

(defn cartesian->polar
  "Converts cartesian cooradinates to the polar ones."
  [[x, y]]
  [(Math/atan2 y x)
   (Math/sqrt (+ (* x x) (* y y)))])

(defn vector-length
  "Returns the length of vtr."
  [vtr]
  (Math/sqrt (reduce #(+ %1 (* %2 %2)) 0.0 vtr)))

(defn normalize-vector
  "Normalizes vector, that is returns a vector with the same
  direction, but with the length equal 1."
  [vtr]
  (let [length (vector-length vtr)]
    (mapv #(/ % length) vtr)))

(defn distance
  "Returns the distance between p1 and p2."
  [p1 p2]
  (vector-length (vector-subtract p1 p2)))

(defn heading
  "Returns the heading from point1 to point2 in radians."
  [point1 point2]
  (Math/atan2 (- (second point2) (second point1)) (- (first point2) (first point1))))

(defn circle-X-circle?
  "Does the first circle intersects the second one?"
  [{position1 :position radius1 :radius}
   {position2 :position radius2 :radius}]
  (<= (distance position1 position2)
      (+ radius1 radius2)))

(defn circle-X-any-circle
  "Returns the first circle the specified circle intersects with."
  [circle other-circles]
  (trace "circle-X-any-circle" circle other-circles)
  (some (fn [c] (if (circle-X-circle? c circle) c nil)) other-circles))

(defn circle-X-rectangle?
  "Does the circle intersects the rectangle?"
  [{[cx, cy] :position radius :radius}
   {[rx, ry] :position [width, height] :size}]
  (and (<= (- rx radius) cx (+ rx width radius))
       (<= (- ry radius) cy (+ ry height radius))))