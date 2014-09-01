(ns wag.routes
  (:require 
    [secretary.core :as secretary :include-macros true]
    [om.core :as om :include-macros true]
    [wag.actions :as actions]
    [wag.views :as views]
    [wag.core]))

(defn init []
  (secretary/defroute "/login" []
                      (actions/login))
  (secretary/defroute "/dashboard" []
                      (actions/dashboard))
  (secretary/defroute "/new-game" []
                      (actions/new-game))
  (secretary/defroute "/join-game" []
                      (actions/join-game)))

(defn dispatch! [path]
  (let [{:keys [template]} (secretary/dispatch! path)]
    (om/root
      template
      wag.core/app-state
      {:target (views/get-by-id "wag-main-container")})))
