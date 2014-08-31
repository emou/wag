(ns wag.routes
  (:require 
    [secretary.core :as secretary :include-macros true]
    [wag.core]
    [wag.views :as views]))

(defn init []
  (secretary/defroute "/login" []
                      {:state {}
                       :template views/login})
  (secretary/defroute "/dashboard" []
                      {:state {}
                       :template views/dashboard})
  (secretary/defroute "/new-game" []
                      {:state {}
                       :template views/new-game}))
