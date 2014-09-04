(ns wag.views
  (:require
    [om.core :as om :include-macros true]
    [secretary.core :as secretary :include-macros true]
    [sablono.core :as html :refer-macros [html]]
    [wag.routes :as routes]
    [wag.state :as state]
    [wag.log :as log]
    [wag.game :as wgame]
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
            (routes/dispatch! "/login")))

(defn on-connection [{:keys [error, session, username]}]
  (if error
    (handle-error error)
    (do
      (.subscribe
        session
        (str "user/" username)
        (fn [topic, event]
          (log/debug "Got private event on " topic event)
          (state/handle-event! event)))

      (.subscribe
        session
        "new-game"
        (fn [topic, event]
          (state/handle-new-game! event)))
      
      (state/set-wamp-session! session)

      (routes/dispatch! "/dashboard"))))

(defn attempt-login [username password]
  (do
    (println "Attempting log in as " username)
    (wamp-client/connect WS_URI username password on-connection)))

(defn login [app owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:form
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
         ]))
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
      (html
        [:div
         [:h4 "Joined games"]

         (let [games (:joined-games app)]
           (if (empty? games)
             [:i "You have not joined any games yet"]
             [:ul {:class "list-group"}
              (map (fn [game]
                     (let [game-id-kw (:id game)
                           game-id (name game-id-kw)]
                       [:li {:key game-id :class "list-group-item"}
                        [:h5 (str game-id " (" (pluralize "player" (wgame/player-count game)) ")")]
                        (str "Created by " (:creator game))
                        [:div {:class "btn-group btn-group-xs game-buttons pull-right"}
                         [:button {:class "btn btn-default"
                                   :on-click #(routes/dispatch! (str "/play-game/" game-id))}
                          "Play"]
                         [:button {:class "btn btn-default"
                                   :on-click #(routes/dispatch! (str "/quit-game/" game-id))}
                          "Quit"]]]))
                   (vals games))]))

         [:button {:class "btn btn-block btn-primary"
                   :on-click #(routes/dispatch! "/new-game")}
          "Start a new game"]

         [:button {:class "btn btn-block btn-primary"
                   :on-click #(routes/dispatch! "/choose-game")}
          "Join another game"]]
         ))))

(defn choose-game [app owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div
         [:h4 "Join an existing game"]
         (let [games (:available-games app)]
           (if (empty? games)
             [:i "No games available for joining"]
             [:ul {:class "list-group"}
              (map (fn [game] 
                     (let [game-id (name (:id game))]
                       [:li {:key game-id :class "list-group-item"}
                        [:h5 (str game-id " (" (pluralize "player" (wgame/player-count game)) ")")]
                        (str "Created by " (:creator game))
                        [:div {:class "btn-group btn-group-xs game-buttons pull-right"}
                         [:button {:class "btn btn-default"
                                   :on-click #(routes/dispatch! (str "/join-game/" game-id))}
                          "Join"]]]
                       )) (vals games))]))]))))

(defn join-game [app owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div
         (let [game-id-kw (:joining-game-id app)
               game-id (name game-id-kw)
               game ((:available-games app) game-id-kw)]
           [:h4 (str "Join " game-id)]
           [:i (str "Creator " (:creator game))])]))))

(defn game-ui [game]
  (reify
    om/IRender
    (render [_]
      (html [:div
             [:h5 (str "Playing game " (:id game))]
             (let [needed-players (wgame/players-needed game)]
               (if (> needed-players 0)
                 [:i (str "Waiting for " needed-players " more players to join ...")]
                 [:i "Ha. game running!"]))]))))

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
      (do
        (if-let [game (get-in app [:joined-games (:played-game-id app)])]
          (om/build game-ui game)
          (html [:h5 "Loading game ..."]))))))

(defn app [app-state owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div
         [:nav {:class "navbar navbar-fixed-top navbar-default"
                :role "navigation"}
          [:div {:class "navbar-header"}
           (when-let [username (:username app-state)]
             [:span {:class "pull-right"} (str "Logged in as " username)])]]
         [:h1 "Word Association Game"]
         [:p
          [:i "A port from real life to Clojure and the web."]]
         (if-let [screen (:screen app-state)]
           (om/build screen app-state)
           nil)]))))

(om/root
  app
  state/app-state
  {:target (get-by-id "wag-main-container")})
