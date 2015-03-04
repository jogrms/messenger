(ns messenger.macros)

(defn- wrap-do [form]
  `(~(first form) (do ~@(rest form))))

(defmacro deflistener [name args & forms]
  (let [cases (->> forms
                   (map wrap-do)
                   (apply concat))]
    `(defn ~name [req#]
       (let [~args [req#]]
         (case (:action req#)
           ~@cases)))))
