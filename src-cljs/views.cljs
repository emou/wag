(ns wag.views
  (:require
    [om.core :as om :include-macros true]
    [secretary.core :as secretary :include-macros true]
    [sablono.core :as html :refer-macros [html]]
    [wag.routes]
    [wag.state]
    [wag.log :as log]
    [wag.wamp-client :as wamp-client]))

(def WS_URI "ws://localhost:8080/ws")

(defn pluralize [word cnt]
  (if (= 1 cnt)
    (str cnt " " word)
    (str cnt " " word "s")))

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
            (wag.routes/dispatch! "/login")))

(defn on-connection [{:keys [error, session, username]}]
  (if error
    (handle-error error)
    (do
      (aset js/window "sess" session)
      (.subscribe
        session
        (str "user/" username)
        (fn [topic, event]
          (wag.state/handle-event! event)
          (.log js/console "Got topic " topic " event " event)))
      
      (wag.state/set-wamp-session! session)
      (wag.routes/dispatch! "/dashboard"))))

(defn attempt-login [username password]
  (do
    (println "Attempting log in as " username)
    (wamp-client/connect WS_URI username password on-connection)))

(defn login [app owner]
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

(defn dashboard [app owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (log/debug "dashboard mount!!"))
    om/IWillUnmount
    (will-unmount [this]
      (log/debug "dashboard unmount!!"))
    om/IRender
    (render [_]
      (render-partial
        app
        [[:h4 "Joined games"]
         (let [games (:joined-games app)]
           (if (empty? games)
             [:i "You have not joined any games yet"]
             [:ul {:class "list-group"}
              (for [[game-id game] games]
                [:li {:key game-id :class "list-group-item"}
                 [:h5 (str game-id " (" (pluralize "player" (count (:players game))) ")")]
                 (str "Created by " (:creator game))
                 [:div {:class "btn-group btn-group-xs game-buttons pull-right"}
                  [:button {:class "btn btn-default"
                            :on-click #(wag.routes/dispatch! (str "/play-game/" game-id))}
                   "Play"]
                  [:button {:class "btn btn-default"
                            :on-click #(wag.routes/dispatch! (str "/quit-game/" game-id))}
                   "Quit"]]])]))

         [:button {:class "btn btn-block btn-primary"
                   :on-click #(wag.routes/dispatch! "/new-game")}
          "Start a new game"]

         [:button {:class "btn btn-block btn-primary"
                   :on-click #(wag.routes/dispatch! "/join-game")}
          "Join another game"]
         ]))))

(defn join-game [app owner]
  (reify
    om/IRender
    (render [_]
      (render-partial
        app
        [[:h4 "Join an existing game by providing pass ID"]]))))

(defn play-game [app owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (log/debug "play-game mount!!"))
    om/IWillUnmount
    (will-unmount [this]
      (log/debug "play-game unmount!!"))
    om/IRender
    (render [_]
      (log/debug "play-game render called")
      (render-partial
        app
        [[:h5 (:id (wag.state/get-played-game))]]))))

(defn app [app-state owner]
  (reify
    om/IRender
    (render [_]
      (html (if-let [screen (:screen app-state)]
              (om/build screen app-state)
              nil)))))

(om/root
  app
  wag.state/app-state
  {:target (get-by-id "wag-main-container")})
