(ns wag.core
  (:require [clojure.browser.repl]
            [wag.routes :as routes]))

(defn init []
  (do
    (enable-console-print!)
    (routes/init)
    (println "Application initialized")
    (println "Dispatching /login")
    (routes/dispatch! "/login")
    ; Skip login. For testing
    (routes/dispatch! "/dashboard")
    ))

(comment
  (ns wag.core)
  (swap! app-state assoc :text ":)")
  (js/alert "hm!")
  (println "x!")
  )
