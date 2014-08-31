(ns wag.views
  (:require
    [om.core :as om :include-macros true]
    [sablono.core :as html :refer-macros [html]]))

(defn login [app]
  (om/component
    (html
      [:div [:h2 "Word Association Game"]
             [:h3 "A port from real life to Clojure and the web."]])))
