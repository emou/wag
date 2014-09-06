(ns wag.state
  (:require [clojure.walk :refer [keywordize-keys]]
            [wag.log :as log]
            [wag.game :as wgame]
            [wag.wamp-client :as wamp-client]))

(def app-state
  "The global application state"
  (atom nil))

(def wamp-session
  "The active WAMP session"
  (atom nil))

(defn- handle-private-game-state! [event]
  "Handle an event with private-state of the game"
  (swap! app-state update-in [:joined-games (:game-id event)] merge
         (select-keys event [:private-state])))

(defn subscribe-to-played-game! []
  "Subscribe to the game that is being played.
  :played-game-id must have been set in app-state."
  (let [session @wamp-session
        username (:username @app-state)
        played-game-id (:played-game-id @app-state)]
    (when (and session username played-game-id)
      (wamp-client/subscribe
        session
        (str "game/" played-game-id "/" username)
        (fn [topic event]
          (log/debug "got private game event " topic event)
          (handle-private-game-state! (keywordize-keys (js->clj event))))))))

(defn handle-reset-state! [{:keys [username session-id games]}]
  "Reset the state, i.e. list of games and initial data"
  (let [by-join (fn [game]
                  (if (wgame/joined? game username)
                    :joined-games
                    :available-games))
        games-by-join (group-by by-join games)
        first-values (fn [m]
                       (into {} (for [[k v] m]
                                  [k (keywordize-keys (first v))])))
        grouped-games (into {} (for [[k v] games-by-join]
                                 [k (first-values (group-by :id v))]))]
    (swap! app-state merge grouped-games)
    (swap! app-state assoc :username username)
    (swap! app-state assoc :session-id session-id))
    (subscribe-to-played-game!)
    (log/debug "Initialized state" @app-state))

(defn handle-event! [js-event]
  "Handle an event from the local"
  (let [event (keywordize-keys (js->clj js-event))
        event-type (:type event)]
    (log/debug "Handing event " js-event)
    (case event-type
      "reset-state" (handle-reset-state! (:state event))
      (log/info "Got unexpected event type" event-type))))

(defn handle-new-game! [js-event]
  "Handle a new-game event by adding the new game to the global state"
  (let [game (keywordize-keys (js->clj js-event))
        game-id (:id game)]
    (if (wgame/joined? game (:username @app-state))
      (swap! app-state assoc-in [:joined-games game-id] game)
      (swap! app-state assoc-in [:available-games game-id] game))))

(defn handle-update-game! [js-event]
  (let [game (keywordize-keys (js->clj js-event))
        game-id (:id game)]
    (if (wgame/joined? game (:username @app-state))
      (do
        (swap! app-state update-in [:joined-games game-id] merge game)
        (swap! app-state update-in [:available-games] dissoc game-id))
      (do
        (swap! app-state update-in [:available-games game-id] dissoc game-id)
        (swap! app-state update-in [:available-games game-id] merge game))))
  (log/debug "After update-game " @app-state))

(defn set-wamp-session! [session]
  (reset! wamp-session session))

(defn get-wamp-session []
  @wamp-session)

(defn set-played-game! [game-id]
  (swap! app-state assoc :played-game-id game-id)
  (subscribe-to-played-game!))

(defn set-joining-game! [game-id]
  (swap! app-state assoc :joining-game-id game-id))

(defn set-screen! [screen]
  "Set the visible screen (view)"
  (swap! app-state assoc :screen screen))
