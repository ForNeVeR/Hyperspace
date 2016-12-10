(ns hyperspace.library.misc
  (:require [clojure.core.typed :as t]))

(t/ann rand-range [t/AnyInteger t/AnyInteger -> t/AnyInteger])
(defn rand-range
  "Returns a random integer between low (inclusive) and
  high (inclusive)"
  [low high]
  (-> (- high low)
      (+ 1)
      rand-int
      (+ low)))

(t/ann saturation [t/AnyInteger t/AnyInteger t/AnyInteger -> t/AnyInteger])
(defn saturation
  "Ensures that the value is not going out of bounds [low, high]"
  [value low high]
  (cond
    (< value low) low
    (< high value) high
    :else value))
