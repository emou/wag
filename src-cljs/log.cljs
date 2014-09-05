(ns wag.log)

(defn info [& args]
  (.apply
    (.-info js/console)
    js/console
    (clj->js args)))

(defn debug [& args]
  (.apply
    (.-debug js/console)
    js/console
    (clj->js args)))

(defn warn [& args]
  (.apply
    (.-warn js/console)
    js/console
    (clj->js args)))
