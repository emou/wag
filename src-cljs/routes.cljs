(ns wag.routes
  (:require [wag.core]
            [secretary.core :as secretary :include-macros true]))

(defn init []
  (secretary/defroute
    "/login" []
    (do
      (println "login route triggered")
      (swap! wag.core/app-state assoc :text "Logging in ..."))))
