(ns wag.log)

(defn info [& args]
  "Log with debug level"
  (.apply
    (.-info js/console)
    js/console
    (clj->js args)))

(defn debug [& args]
  "Log with debug level"
  (.apply
    (.-debug js/console)
    js/console
    (clj->js args)))

(defn warn [& args]
  "Log with warn level"
  (.apply
    (.-warn js/console)
    js/console
    (clj->js args)))

(defn error [& args]
  "Log with error level"
  (.apply
    (.-error js/console)
    js/console
    (clj->js args)))
