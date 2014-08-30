(ns wag.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.browser.repl]))

(enable-console-print!)

(def app-state (atom {:loggedIn false :text "Login"}))

(defn init []
  (do
    (println "wag.core initialized")
    (println "logged in: " (:loggedIn @app-state))))

(om/root
  (fn [app owner]
    (reify om/IRender
      (render [_]
        (dom/h1 nil (:text app)))))
  app-state
  {:target (. js/document (getElementById "app"))})

(init)

(comment
  (ns wag.core)
  (swap! app-state assoc :text ":)")
  (js/alert "hm!")
  (println "x!")
  (map)
  )
