(ns wag.state
  (:require [clojure.walk :refer [keywordize-keys]]
            [wag.log :as log]
            [wag.game :as wgame]))

(def app-state (atom nil))
(def wamp-session (atom nil))

(defn reset-state! [{:keys [username session-id games]}]
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
    (log/debug "initialized state" @app-state))

(defn handle-event! [js-event]
  (let [event (keywordize-keys (js->clj js-event))
        event-type (:type event)]
    (log/debug "Handing private event " js-event)
    (case event-type
      "reset-state" (reset-state! (:state event))
      (log/info "Got unexpected event type" event-type))))

(defn handle-new-game! [js-event]
  (log/debug "Handling new game " js-event)
  (let [game (keywordize-keys (js->clj js-event))
        game-id (:id game)]
    (if (wgame/joined? game (:username @app-state))
      (swap! app-state assoc-in [:joined-games game-id] game)
      (swap! app-state assoc-in [:available-games game-id] game)))
  (log/debug "After new-game " @app-state))

(defn handle-update-game! [js-event]
  (log/debug "Handling game update " js-event)
  (let [game (keywordize-keys (js->clj js-event))
        game-id (:id game)]
    (if (wgame/joined? game (:username @app-state))
      (do
        (swap! app-state update-in [:joined-games game-id] merge game)
        (swap! app-state dissoc-in [:available-games game-id]))
      (do
        (swap! app-state dissoc-in [:available-games game-id])
        (swap! app-state update-in [:available-games game-id] merge game))))
  (log/debug "After update-game " @app-state))

(defn set-wamp-session! [session]
  (reset! wag.state/wamp-session session))

(defn get-wamp-session []
  @wamp-session)

(defn set-played-game! [game-id]
  (swap! app-state assoc :played-game-id game-id))

(defn set-joining-game! [game-id]
  (swap! app-state assoc :joining-game-id game-id))

(defn set-screen! [screen]
  (swap! app-state assoc :screen screen))
