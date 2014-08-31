(ns wag.routes
  (:require [secretary.core :as secretary :include-macros true]))

(secretary/defroute "/login" [] (do
                                  (println "login")
                                  (swap! app-state assoc :text "Login!")))
