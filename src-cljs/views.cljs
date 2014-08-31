(ns wag.views
  (:require
    [om.core :as om :include-macros true]
    [sablono.core :as html :refer-macros [html]]))

(def header
  [:div {:class "container"}
   [:h1 "Word Association Game"]
   [:p
    [:i "A port from real life to Clojure and the web."]]])

(defn render-partial [partial-html]
  (om/component
    (html
      (into  header partial-html))))

(defn login [app]
  (render-partial
    [[:form
      {:id "login-form"
       :action "#"
       :class "form-signin"
       :role "form"}

      [:input
       {:id "login-username"
        :type "text"
        :class "form-control"
        :placeholder "Username"}]

      ]]))

;; <form id="login-form" action="#" class="form-signin" role="form">
;;   <input id="login-username" type="text" class="form-control" placeholder="Username">
;;   <input id="login-password" type="password" class="form-control" placeholder="Password">
;;   <button class="btn btn-lg btn-primary btn-block" type="submit">Sign in</button>
;; </form>
