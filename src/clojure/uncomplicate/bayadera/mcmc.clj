(ns ^{:author "Dragan Djuric"}
    uncomplicate.bayadera.mcmc
  (:require [uncomplicate.neanderthal
             [core :refer [iamax]]
             [real :refer [entry]]
             [math :refer [pow sqrt abs]]]
            [uncomplicate.bayadera
             [protocols :as p]
             [util :refer [srand-int]]]))

(defn init-position!
  ([samp position]
   (p/init-position! samp position))
  ([samp seed limits]
   (p/init-position! samp seed limits)))

(defn acc-rate!
  (^double [samp a]
   (double (p/acc-rate! samp a)))
  (^double [samp]
   (double (p/acc-rate! samp 2.0))))

(defn burn-in!
  ([samp ^long steps ^double a]
   (p/burn-in! samp steps a))
  ([samp ^long steps]
   (p/burn-in! samp steps 2.0))
  ([samp]
   (p/burn-in! samp (* 256 (long (p/dimension (p/model samp)))) 2.0)))

(defn run-sampler!
  ([samp ^long steps ^double a]
   (p/run-sampler! samp steps a))
  ([samp ^long steps]
   (p/run-sampler! samp steps 2.0))
  ([samp]
   (p/run-sampler! samp (* 64 (long (p/dimension (p/model samp)))) 2.0)))

(defn sqrt-n [^double temp]
  (fn ^double [^long i]
    (Math/sqrt (- temp i))))

(defn minus-n [^double temp]
  (fn ^double [^long i]
    (- temp i)))

(defn anneal!
  ([samp schedule ^long steps ^double a]
   (p/anneal! samp (schedule steps) steps a))
  ([samp ^long steps ^double a]
   (p/anneal! samp (minus-n steps) steps a))
  ([samp ^long steps]
   (anneal! samp steps 2.0))
  ([samp]
   (anneal! samp (* 256 (long (p/dimension (p/model samp)))) 2.0)))

(defn mix!
  ([samp options]
   (let [{step :step
          position :position
          schedule :cooling-schedule
          a :a
          refining :refining
          min-acc :min-acc-rate
          max-acc :max-acc-rate
          :or {step 64
               schedule minus-n
               position (srand-int)
               a 2.0
               refining 0
               min-acc 0.2
               max-acc 0.5}} options
         a (double a)
         refining (long refining)
         min-acc (double min-acc)
         max-acc (double max-acc)
         target-acc (/ (+ max-acc min-acc) 2.0)
         dimension (long (p/dimension (p/model samp)))
         step (long step)]
     (anneal! samp schedule (* step (pow dimension 0.8)) a)
     (let [a (loop [i 0 a a]
               (let [acc-rate (acc-rate! samp a)]
                 (cond
                   (< step i) a
                   (< acc-rate min-acc)
                   (recur (inc i) (inc (* (dec a) (/ acc-rate target-acc))))
                   (< max-acc acc-rate)
                   (recur (inc i) (* a (/ acc-rate target-acc)))
                   :default a)))]
       (burn-in! samp (* step (pow dimension 0.8)) a)
       (loop [i 0 diff (abs (- target-acc (acc-rate! samp a)))]
         (when (< i refining)
           (burn-in! samp (* step (pow dimension 0.8)) a)
           (let [new-diff  (abs (- target-acc (acc-rate! samp a)))]
             (when (< new-diff diff)
               (recur (inc i) new-diff)))))
       (run-sampler! samp step a))))
  ([samp]
   (mix! samp nil)))

(defn info [samp]
  (p/info samp))
