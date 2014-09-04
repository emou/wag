(ns wag.routes
  (:require 
    [secretary.core :as secretary :include-macros true]
    [om.core :as om :include-macros true]
    [wag.state :as state]
    [wag.actions :as actions]
    [wag.views :as views]
    [wag.log :as log]))

(defn init []
  (secretary/defroute "/login" []
                      (actions/login))
  (secretary/defroute "/dashboard" []
                      (actions/dashboard))
  (secretary/defroute "/new-game" []
                      (actions/new-game))
  (secretary/defroute "/join-game" []
                      (actions/join-game))
  (secretary/defroute "/play-game/:id" {:as params}
                        (actions/play-game (keyword (:id params)))))

(defn dispatch! [path]
  (log/info "Dispatching " path)
  (let [{:keys [view]} (secretary/dispatch! path)]
    (wag.state/set-screen! view)))
