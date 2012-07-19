(ns hyperspace.ui
  (:use (hyperspace game
                    geometry
                    world))
  (:import (org.lwjgl Sys)
           (org.lwjgl.input Keyboard)
           (org.lwjgl.opengl Display
                             DisplayMode
                             GL11)
           (hyperspace.world Bullet
                             Planet
                             Player
                             World)))

(declare start-ui)
(declare setup-ui)
(declare get-time)
(declare ui-loop)
(declare clear-display)
(declare process-input)
(defmulti render class)

(def window-width 800)
(def window-height 600)
(def fps 60)
(def scale 1) ; pixels per kilometer
(def time-scale 10) ; rounds per second

(defn start-ui
  "Starts UI."
  [world]
  (setup-ui)
  (ui-loop world))

(defn setup-ui
  []
  (Display/setDisplayMode (DisplayMode. window-width window-height))
  (Display/create)
  (Keyboard/enableRepeatEvents true)
  (GL11/glClearColor 0 0 0 0)
  (GL11/glViewport 0 0 window-width window-height)
  (GL11/glEnable GL11/GL_BLEND)
  (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA))

(defn get-time
  []
  (/ (Sys/getTime) (Sys/getTimerResolution)))

(defn ui-loop
  [init-world]
  (loop [time (get-time)
         world init-world]
    (clear-display)
    (render world)
    (let [world (process-input world)]
      (if (Display/isCloseRequested)
        (Display/destroy)
        (let [new-time (get-time)
              time-delta (- new-time time)
              [new-world remaining-time] (update-world world (* time-delta time-scale))]
          (Display/sync fps)
          (Display/update)
          (recur (+ new-time remaining-time)
                 new-world))))))

(defn clear-display
  []
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT)))

(defn process-input
  [world]
  (if (and (Keyboard/next)
           (Keyboard/getEventKeyState))
    (let [key (Keyboard/getEventKey)
          {[player & _] :players
           bullets      :bullets
           traces       :traces} world
          {name    :name
           center  :center
           power   :power
           heading :heading} player]
      (cond
        (= key Keyboard/KEY_UP)
          (update-player-params world name heading (+ power 0.05))
        (= key Keyboard/KEY_DOWN)
          (update-player-params world name heading (- power 0.05))
        (= key Keyboard/KEY_LEFT)
          (update-player-params world name (+ heading 0.1) power)
        (= key Keyboard/KEY_RIGHT)
          (update-player-params world name (- heading 0.1) power)
        (and (= key Keyboard/KEY_SPACE)
             (not (Keyboard/isRepeatEvent)))
          (fire world name)
        :else world))
  world))

(defn draw-ellipse
  [x y a b segments]
  (let [delta-angle (/ (* 2 Math/PI) segments)]
    (GL11/glBegin GL11/GL_POLYGON)
    (doseq [angle (->> 0.0
                       (iterate (partial + delta-angle))
                       (take segments))]
      (GL11/glVertex2f (+ (* a (Math/cos angle)) x)
                       (+ (* b (Math/sin angle)) y)))
    (GL11/glEnd)))

(defn normalize-x
  [x]
  (/ x window-width))

(defn normalize-y
  [y]
  (/ y window-height))

(defn space-point-to-display
  [{x :x y :y :as point}]
  (assoc point
    :x (- (* 2 (normalize-x (* x scale))) 1)
    :y (- (* 2 (normalize-y (* y scale))) 1)))

(defn draw-crosshair
  [player]
  (GL11/glColor3f 1 0.5 0.5)
  (let [{{center-x :x center-y :y} :center
         heading                   :heading
         power                     :power} player

        x (+ center-x (* (Math/cos heading) power 20))
        y (+ center-y (* (Math/sin heading) power 20))

        point (make-point x y)

        display-point (space-point-to-display point)
        display-x (:x display-point)
        display-y (:y display-point)]
    (draw-ellipse display-x display-y (normalize-x 5) (normalize-y 5) 30)))

(defn space-point-on-display?
  [{x :x, y :y}]
  (and (< 0 x window-width)
       (< 0 y window-height)))

(defn draw-line [x1 y1 x2 y2]
  (GL11/glBegin GL11/GL_LINE_STRIP)
  (GL11/glVertex2d x1 y1)
  (GL11/glVertex2d x2 y2)
  (GL11/glEnd))

(defn draw-lines
  [points]
  (GL11/glBegin GL11/GL_LINES)
  (doseq [[{x1 :x y1 :y} {x2 :x y2 :y}] (map vector points (rest points))]
    (GL11/glVertex2d x1 y1)
    (GL11/glVertex2d x2 y2))
  (GL11/glEnd))

(defn bullet-trace-color-alpha
  [bullet]
  (let [idx (:index bullet)
        c (- 1 (/ idx max-bullets))
        c (max 0 (min c 1))]
    (Math/pow c 0.7)))

(defn draw-traces
  [bullet]
  (let [traces (:traces bullet)]
    (when (seq traces)
      (let [alpha (bullet-trace-color-alpha bullet)]
        (GL11/glColor4f 1 1 0.6 alpha))
      (let [points (concat (take-nth 8 traces) [(last traces)])]
        (draw-lines (map space-point-to-display points))))))

(defmethod render Planet
  [planet]
  (GL11/glColor3f 0 1 0)
  (let [center (space-point-to-display (:center planet))
        {center-x :x center-y :y} center
        radius-px (* (:radius planet) scale)
        radius-x (* 2 (normalize-x radius-px))
        radius-y (* 2 (normalize-y radius-px))]
    (draw-ellipse center-x center-y radius-x radius-y 30)))

(defmethod render Player
  [player]
  (GL11/glColor3f 0 0 1)
  (let [center (space-point-to-display (:center player))
        {center-x :x center-y :y} center
        x1 (- center-x (normalize-x 20))
        y1 (- center-y (normalize-y 20))
        x2 (+ center-x (normalize-x 20))
        y2 (+ center-y (normalize-y 20))]
    (GL11/glRectf x1 y1 x2 y2))
  (draw-crosshair player))

(defmethod render Bullet
  [bullet]
  (draw-traces bullet)
  (when (bullet-alive? bullet)
    (GL11/glColor3f 1 0 0)
    (let [center (space-point-to-display (:center bullet))
          {center-x :x center-y :y} center
          x1 (- center-x (normalize-x 7))
          y1 (- center-y (normalize-y 7))
          x2 (+ center-x (normalize-x 7))
          y2 (+ center-y (normalize-y 7))]
      (GL11/glRectf x1 y1 x2 y2))))

(defmethod render World
  [{planets :planets
    players :players
    bullets :bullets}]
  (dorun (map render (concat planets players bullets))))
