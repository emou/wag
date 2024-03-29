(ns wag.core
  (:require [clojure.browser.repl]
            [wag.routes :as routes]
            [wag.views :as views]
            [wag.log :as log]
            [wag.actions :as actions])
  (:import [goog Uri]))

(def username
  "Read the username from the location bar. Used for auto-login for easier
  testing."
  (->
    (new Uri (.-location js/window))
    (.getParameterValue "user")))

(defn init []
  (do
    (enable-console-print!)
    (routes/init)
    (log/debug "Application initialized")
    (routes/dispatch! "/login")
    ;; Auto-login. For easier testing.
    (actions/attempt-login (or username "guest") "1")))

(comment
  (ns wag.core)
  (swap! wag.state/app-state assoc :text ":)")
  (js/alert "hm!")
  (println "x!"))
