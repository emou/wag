(ns wag.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary :include-macros true]
            [clojure.browser.repl]
            [wag.routes :as routes]
            [wag.views :as views]))

(defn dispatch! [path]
  (let [{:keys [template, state]} (secretary/dispatch! path)]
    (om/root
      template
      state
      {:target (views/get-by-id "wag-main-container")})))

(defn init []
  (do
    (enable-console-print!)
    (routes/init)
    (println "Application initialized")
    (println "Dispatching /login")
    (dispatch! "/login")))

(comment
  (ns wag.core)
  (swap! app-state assoc :text ":)")
  (js/alert "hm!")
  (println "x!")
  )
