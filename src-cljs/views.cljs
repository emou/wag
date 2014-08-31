(ns wag.views
  (:require
    [om.core :as om :include-macros true]
    [secretary.core :as secretary :include-macros true]
    [sablono.core :as html :refer-macros [html]]
    [wag.core]
    [wag.wamp-client :as wamp-client]))

(def WS_URI "ws://localhost:8080/ws")
(def BASE_TOPIC_URI "http://wag/")

(defn header [app]
  [:div
   [:h1 "Word Association Game"]
   [:p
    [:i "A port from real life to Clojure and the web."]]])

(defn render-partial [app partial-html]
  (html
    (into (header app) partial-html)))

(defn get-by-id [id]
  (.getElementById js/document id))

(defn value-by-id [id]
  (.-value (wag.views/get-by-id id)))

(defn error-message [error]
  (case (:type error)
    :auth "Wrong username or password. Please try again"
    "Could not connect to the server. Please try again later"))

(defn handle-error [error]
  (js/alert (error-message error)
            (wag.core/dispatch! "/login")))

(defn on-connection [{:keys [error, session]}]
  (if error
    (handle-error error)
    (do
      (println "Authenticated session " session)
      (wag.core/dispatch! "/dashboard"))))

(defn attempt-login [username password]
  (do
    (println "Attempting log in as " username)
    (wamp-client/connect WS_URI BASE_TOPIC_URI
                         username password on-connection)))

(defn login [app]
  (reify
    om/IRender
    (render [_]
      (render-partial
        app
        [[:form
          {:id "login-form"
           :action "#"
           :class "form-signin"
           :role "form"
           :on-submit #(attempt-login
                         (value-by-id "login-username")
                         (value-by-id "login-password"))}

          [:input
           {:id "login-username"
            :type "text"
            :class "form-control"
            :placeholder "Username"}]

          [:input
           {:id "login-password"
            :type "password"
            :class "form-control"
            :placeholder "Password"}]

          [:button
           {:class "btn btn-lg btn-primary btn-block"
            :type "submit"} "Sign in"]
          ]]))
    ))

(defn new-game [])

(defn dashboard [app]
  (reify
    om/IRender
    (render [_]
      (render-partial
        app
        [[:h4 "Dashboard"]
         [:a {:href "#"
              :on-click new-game}
          "New game"]
         ]))))
