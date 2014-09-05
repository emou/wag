(ns wag.core
  (:require [clojure.browser.repl]
            [wag.routes :as routes]
            [wag.views :as views]
            [wag.log :as log]
            [wag.actions :as actions])
  (:import [goog Uri]))

(def username
  (->
    (new Uri (.-location js/window))
    (.getParameterValue "user")))

(defn init []
  (do
    (enable-console-print!)
    (routes/init)
    (log/debug "Application initialized")
    (routes/dispatch! "/login")
    (actions/attempt-login (or username "guest") "1") ; Auto-login. For easier testing.
    ))

(comment
  (ns wag.core)
  (swap! wag.state/app-state assoc :text ":)")
  (js/alert "hm!")
  (println "x!")
  )
