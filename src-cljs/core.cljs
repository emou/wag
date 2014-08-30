(ns wag.core)

(enable-console-print!)

(def app-state (atom {:loggedIn false}))

(defn ^:export init []
  (do
    (println "wag.core initialized")
    (println "logged in: " (:loggedIn @app-state))))
