(ns gen-art.rotating-lines-noise-grid
  (:use [rosado.processing]
        [rosado.processing.applet]
        [gen-art.util :only [range-incl mul-add]]))

;; Section 5.1.2 (figure 5.4) p88

;; float xstart, xnoise, ynoise;

;; void setup() {
;;   size(300, 300);
;;   smooth();
;;   background(255);
;;   xstart = random(10);
;;   xnoise = xstart;
;;   ynoise = random(10);
;;   for(int y = 0; y <= height; y+=5) {
;;     ynoise += 0.1;
;;     xnoise = xstart;
;;     for(int x = 0; x <= width; x+=5) {
;;       xnoise += 0.1;
;;       drawPoint(x, y, noise(xnoise, ynoise));
;;     }
;;   }
;; }

;; void drawPoint(float x, float y, float noiseFactor) {
;;   pushMatrix();
;;   translate(x,y);
;;   rotate(noiseFactor * radians(540));
;;   float edgeSize = noiseFactor * 35;
;;   float grey = 150 + (noiseFactor * 120);
;;   float alph = 150 + (noiseFactor * 120);
;;   noStroke();
;;   fill(grey, alph);
;;   ellipse(0,0, edgeSize, edgeSize/2);
;;   popMatrix();
;; }


(defn draw-point
  [x y noise-factor]
  (push-matrix)
  (translate x y)
  (rotate (* noise-factor (radians 540)))
  (let [edge-size (* noise-factor 35)
        grey (mul-add noise-factor 120 150)
        alph (mul-add noise-factor 120 150)]
    (no-stroke)
    (fill grey alph)
    (ellipse 0 0 edge-size (/ edge-size 2))
    (pop-matrix)))

(defn draw-squares
  [x-start y-start]
  (dorun
   (for [y (range-incl 0 (height) 5)
         x (range-incl 0 (width) 5)]
     (let [x-noise (mul-add 0.01 x-start x)
           y-noise (mul-add 0.01 y-start y)
           alph    (* 255 (noise x-noise y-noise))]
       (draw-point x y (noise x-noise y-noise))))))

(defn setup []
  (size 300 300)
  (smooth)
  (background 255)
  (draw-squares (random 10) (random 10)))

(defapplet example
  :title "Fluffy Clouds 2D Noise Grid"
  :setup setup
  :size [300 300])

(run example :interactive)