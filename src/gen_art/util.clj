(ns gen-art.util
  (:use [processing.core]))

(defn line-join-points
  "takes either a seq of x y (and z) point coords tuples or two
  separate lists of x and y (and z) coords independently and creates a
  lazy list of line args (vectors of 4 (or 6) elements) suitable for
  use with the line fn.

  (line-join-points [1 2 3] [4 5 6])     ;=> ([1 4 2 5] [2 5 3 6])
  (line-join-points [[1 4] [2 5] [3 6]]) ;=> ([1 4 2 5] [2 5 3 6])
  (line-join-points [[1 4 0] [2 5 1] [3 6 2]]) ;=> ([1 4 0 2 5 1]
                                                    [2 5 1 3 6 2])
  (line-join-points [1 2 3] [4 5 6] [0 1 2])   ;=> ([1 4 0 2 5 1]
                                                    [2 5 1 3 6 2])"
  ([interleaved-points]
     (lazy-seq
       (let [head (take 2 interleaved-points)]
         (if (= 2 (count head))
           (cons (apply concat head) (line-join-points (drop 1 interleaved-points)))))))
  ([xs ys]
     (lazy-seq
      (if (and (next xs) (next ys))
        (cons [(first xs) (first ys) (second xs) (second ys)]
              (line-join-points (next xs) (next ys)))
        [])))
  ([xs ys zs]
     (lazy-seq
       (if (and (next xs) (next ys) (next zs))
         (cons [(first xs) (first ys) (first zs) (second xs) (second ys) (second zs)]
               (line-join-points (next xs) (next ys) (next zs)))
         []))))

(defn perlin-noise-seq
  "Generate a lazy infinite sequence of perlin noise values starting from
  the specified seed with incr added to the seed for each successive value."
  [seed incr]
  (lazy-seq (cons (noise seed) (perlin-noise-seq (+ seed incr) incr))))

(defn mul-add
  "Generate a potential lazy sequence of values which is the result of
   multiplying each s by mul and then adding add. s mul and add may be
   seqs in which case the result will also be seq with the length
   being the same as the shortest input seq (similar to the behaviour
   of map). If all the seqs passed are infinite lazy seqs, the result
   will also be infinite and lazy..

   (mul-add 2 2 1)           ;=> 5
   (mul-add [2 2] 2 1)       ;=> [5 5]
   (mul-add [2 2] [2 4 6] 1) ;=> [5 9]
   (mul-add (range) 2 1)     ;=> [1 3 5 7 9 11 13...] ;; infinite seq
   (mul-add (range) [2 2] 1) ;=> [1 3]"
  [s mul add]
  (if (and (number? mul) (number? add) (number? s))
    (+ add (* mul s))
    (let [[mul nxt-mul] (if (sequential? mul)
                          [(first mul) (next mul)]
                          [mul mul])
          [add nxt-add] (if (sequential? add)
                          [(first add) (next add)]
                          [add add])
          [s nxt-s]     (if (sequential? s)
                          [(first s) (next s)]
                          [s s])]
      (lazy-seq
       (cons (+ add (* mul s)) (if (and nxt-mul nxt-add nxt-s)
                                 (mul-add  nxt-s nxt-mul nxt-add)
                                 []))))))

(defn range-incl
  "Returns a lazy seq of nums from start (inclusive) to end
  (inclusive), by step, where start defaults to 0, end to infinity and
  step to 1 or -1 depending on whether end is greater than or less
  than start respectively."
  ([] (range-incl 0 Double/POSITIVE_INFINITY))
  ([end] (range-incl 0 end))
  ([start end] (if (< start end)
                 (range-incl start end 1)
                 (range-incl start end -1)))
  ([start end step]
   (lazy-seq
    (let [b (chunk-buffer 32)
          comp (if (pos? step) <= >=)]
      (loop [i start]
        (if (and (< (count b) 32)
                 (comp i end))
          (do
            (chunk-append b i)
            (recur (+ i step)))
          (chunk-cons (chunk b)
                      (when (comp i end)
                        (range-incl i end step)))))))))

(defn indexed-range-incl
  "Returns a sequence of [idx val] pairs over the specified inclusive
  range"
  ([] (indexed-range-incl 0 Double/POSITIVE_INFINITY))
  ([end] (indexed-range-incl 0 end))
  ([start end]
     (if (< start end)
       (indexed-range-incl start end 1)
       (indexed-range-incl start end -1)))
  ([start end step]
     (map list (range) (range-incl start end step))))



(defn indexed-range
  "Returns a sequence of [idx val] pairs over the specified range"
  ([] (indexed-range 0 Double/POSITIVE_INFINITY))
  ([end] (indexed-range 0 end))
  ([start end]
     (if (< start end)
       (indexed-range start end 1)
       (indexed-range start end -1)))
  ([start end step]
     (map list (range) (range start end step))))

(defn steps
  "Returns a lazy sequence of numbers starting at
  start (default 0) with successive additions of step. step may be a
  sequence of steps to apply."
  ([] (steps 1))
  ([step] (steps 0 step))
  ([start step]
     (let [[step next-step] (if (sequential? step)
                              [(first step) (next step)]
                              [step step])]
       (lazy-seq (cons start (if next-step
                               (steps (+ step start) next-step)
                               [(+ step start)]))))))

(defn cycle-between
  "Cycle between min and max with inc-step and dec-step starting at
  start in direction :up"
  ([min max] (cycle-between min min max 1 1))
  ([min max inc-step] (cycle-between min min max inc-step inc-step))
  ([min max inc-step dec-step] (cycle-between min min max inc-step dec-step))
  ([start min max inc-step dec-step] (cycle-between start min max inc-step dec-step :up))
  ([start min max inc-step dec-step direction]
     (let [inc-step (if (neg? inc-step) (* -1 inc-step) inc-step)
           dec-step (if (neg? dec-step) (* -1 dec-step) dec-step)
           next (if (= :up direction)
                  (+ start inc-step)
                  (- start dec-step))
           [next dir] (if (= :up direction)
                        (if (> next max) [(- start dec-step) :down] [next :up])
                        (if (< next min) [(+ start inc-step) :up] [next :down]))]
       (lazy-seq (cons start (cycle-between next min max inc-step dec-step dir))))))

(defn tap
  "Debug tool for lazy sequences. Apply to a lazy-seq to print out
  current value when each element of the sequence is evaluated."
  ([s] (tap "-->" s))
  ([msg s]
     (map #(do (println (str msg " " %)) %) s)))


(defn- swap-returning-prev!
  "Similar to swap! except returns vector containing the previous and new values

  (def a (atom 0))
  (swap-returning-prev! a inc) ;=> [0 1]"
  [atom f & args]
  (loop []
    (let [old-val  @atom
          new-val  (apply f (cons old-val args))
          success? (compare-and-set! atom old-val new-val)]
      (if success?
        [old-val new-val]
        (recur)))))

(defn seq->stream
  "Converts a sequence to a stream - a stateful function which returns
  each subequent element each time it is called

  (def s (seq->stream [1 2 3]))
  (s) ;=> 1
  (s) ;=> 2
  (s) ;=> 3
  (s) ;=> nil"
  [s]
  (let [state (atom (seq s))]
    (fn []
      (let [[old new] (swap-returning-prev! state rest)]
        (first old)))))

(defn tally
  "Cumulative tally. Takes a sequence of numbers and returns a new
  sequence which is a cumulative tally of the successive additions of
  each element in the original seq.

  (take 5 (tally (range))) ;=> [0 1 3 6 10]"
  ([s] (tally s 0))
  ([s amount]
     (lazy-seq
      (let [nxt-amount (+ (first s) amount)
            nxt-s (next s)]
        (cons nxt-amount (if nxt-s
                           (tally nxt-s nxt-amount)
                           []))))))

(defn mod-range
  "Similar to mod except allows min to be non-zero. Always returns a
  val within the range min (inclusive) to max (exclusive). Throws an
  exception if min is greater than max."
  [val min max]
  (when (> min max)
    (throw (Exception. (str "Error in mod-range: min is greater than max (> " min " " max ")"))))
  (if (< min 0)
    (let [abs-min (* -1 min)
          res (mod val (+ max abs-min))]
      (- res abs-min))
    (let [res (mod val (- max min))]
      (+ res min))))
