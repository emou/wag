(ns wag.views
  (:require
    [om.core :as om :include-macros true]
    [secretary.core :as secretary :include-macros true]
    [sablono.core :as html :refer-macros [html]]
    [clojure.string :as string]
    [wag.actions :as actions]
    [wag.game :as wgame]
    [wag.log :as log]
    [wag.routes :as routes]
    [wag.state :as state]
    [wag.wamp-client :as wamp-client]))

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
          :on-submit #(actions/attempt-login
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
                     (let [game-id (:id game)]
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
                     (let [game-id (:id game)]
                       [:li {:key game-id :class "list-group-item"}
                        [:h5 (str game-id " (" (pluralize "player" (wgame/player-count game)) ")")]
                        (str "Created by " (:creator game))
                        [:div {:class "btn-group btn-group-xs game-buttons pull-right"}
                         [:button {:class "btn btn-default"
                                   :on-click #(routes/dispatch! (str "/join-game/" game-id))}
                          "Join"]]])) (vals games))]))]))))

(defn- join-button [game team-kw]
  (let [game-id (:id game)
        team-name (name team-kw)]
    [:button {:class "team-join-button btn btn-xs"
              :on-click #(routes/dispatch! (str "/game/" game-id "/team/" team-name "/join"))}

     "Join"]))

(defn- player-list [heading game team-kw]
  [:ul heading (when-not (wgame/team-full? game team-kw) (join-button game team-kw))
   (map (fn [p]
          [:li {:key p} p])
        (game team-kw))])

(defn join-game [app owner]
  (reify
    om/IRender
    (render [_]
      (html
        (let [game-id (:joining-game-id app)
              game ((:available-games app) game-id)]
          [:div
           [:h4 (str "Join " game-id)]
           [:h6 (str "Created by " (:creator game))]
           (player-list "Team A" game :team-a)
           (player-list "Team B" game :team-b)
           ])))))

(defn turn-input-placeholder [turn teammate]
  (str "You need to " (:type turn) " to " teammate))

(defn own-turn-description [own-turn teammate]
  (let [turn-type (:type own-turn)]
    [:div
     [:h3 "It's your turn!"]
     [:form
      {:id "own-turn"
       :action "#"
       :class ""
       :role "form"
       :on-submit (fn [e]
                    (actions/make-turn {:type turn-type
                                        :value (value-by-id "own-turn-text")})
                    false)}
      [:input
       {:id "own-turn-text"
        :type "text"
        :class "form-control"
        :placeholder (turn-input-placeholder own-turn teammate)}]]]))

(defn teammate-display [teammate my-username]
  (if (= teammate my-username)
    (str "you, " teammate)
    teammate))

(defn next-turn-description [game]
  (let [private-state (:private-state game)
        turn (:next-turn private-state)
        from-user (:from turn)
        teammate (wgame/teammate game from-user)
        my-username (:username @state/app-state)] ; XXX: Don't access global state
    [:div
     (if (= from-user my-username)
       [:h4 (own-turn-description turn teammate)]
       [:h4 (str "Waiting for " from-user
                 " to " (:type turn)
                 " to " (teammate-display teammate my-username)
                 "...")])]))

(defn format-turn-history-entry [turn teammate]
  (log/debug "turn " turn)
  (case (:type turn)
    "hint" (str (:from turn) " hinted " teammate " with " (:value turn))
    "guess" (str (:from turn) " guessed " (:value turn))
    "secret" (str (:from turn) " shared the secret with " teammate)
    "Unkown turn"))

(defn turn-history [turn-history game]
  (for [turn turn-history]
    [:div {:class "alert alert-success"
           :role "alert"} 
     (format-turn-history-entry turn (wgame/teammate game (:from turn)))]))

(defn game-ui [game]
  (reify
    om/IRender
    (render [_]
      (html [:div
             [:h5 (str "Playing game " (:id game))]
             (let [needed-players (wgame/players-needed game)
                   private-state (:private-state game)
                   winner (:winner private-state)]
               (if (> needed-players 0)
                 [:i (str "Waiting for " needed-players " more players to join ...")]
                 [:div (if winner
                         [:p (str winner " (" (string/join " and " ((keyword winner) game)) ") won! The word was ")
                                   [:b (:secret private-state)]]
                         [:div (next-turn-description game)])
                  (turn-history (reverse (:turn-history private-state)) game)]))]))))

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
