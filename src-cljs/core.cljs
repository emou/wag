(ns wag.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary :include-macros true]
            [clojure.browser.repl]
            [wag.views :as views]))

(enable-console-print!)

(def app-state (atom nil))

(defn init []
  (do
    (reset! app-state {:loggedIn false :text "Login"})
    (om/root
      views/login
      app-state
      {:target (. js/document (getElementById "app"))})
    (secretary/dispatch! "/login")
    (println "WAG initialized")
    (println "Dispatching login")))

(init)

(comment
  (ns wag.core)
  (swap! app-state assoc :text ":)")
  (js/alert "hm!")
  (println "x!")
  (map)
  )
