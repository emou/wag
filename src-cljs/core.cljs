(ns wag.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary :include-macros true]
            [clojure.browser.repl]
            [wag.routes :as routes]
            [wag.views :as views]))

(def app-state (atom nil))

(defn init []
  (do
    (enable-console-print!)
    (reset! app-state {:loggedIn false})
    (routes/init)
    (om/root
      views/login
      app-state
      {:target (views/get-by-id "wag-main-container")})
    (println "WAG initialized")
    (println "Dispatching login"))
  (secretary/dispatch! "/login"))

(comment
  (ns wag.core)
  (swap! app-state assoc :text ":)")
  (js/alert "hm!")
  (println "x!")
  )
