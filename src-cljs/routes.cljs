(ns wag.routes
  (:require 
    [secretary.core :as secretary :include-macros true]
    [wag.core]
    [wag.views :as views]))

(defn init []
  (secretary/defroute "/login" []
                      {:state {:logged-in false}
                       :template views/login})
  (secretary/defroute "/dashboard" []
                      {:state {:logged-in true}
                       :template views/dashboard}))
