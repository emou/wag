(ns wag.websocket
  (:use [wag.config :only [conf]])
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server :as http-kit]
            [clj-wamp.server :as wamp]
            [wag.game :as wgame]
            [wag.state :as state]))

;; HTTP Kit/WAMP WebSocket handler

(defn- on-open [sess-id]
  (log/info "WAMP client connected [" sess-id "]"))

(defn- on-close [sess-id status]
  (log/info "WAMP client disconnected [" sess-id "] " status))

(defn- on-publish [sess-id topic event exclude include]
  (log/info "WAMP publish:" sess-id topic event exclude include))

;; TODO: Implement database-backed authentication
;; Currently all users can login using the hardcoded password below.
(defn- auth-secret [sess-id auth-key extra]
  "Returns the auth key's secret (ie. password), typically retrieved from a database."
  "1")

(defn- user-private-channel-url [username]
  (str "user/" username))

(defn- game-private-channel-url [game-id username]
  (str "game/" game-id "/private/" username))

(defn- on-user-authenticated [sess-id username]
  (state/add-user! sess-id username))

(defn- auth-permissions
  "Returns the permissions for a client session by auth key."
  [sess-id auth-key]

  (let [user-private-url (user-private-channel-url auth-key)]
    (on-user-authenticated sess-id auth-key)
    {:subscribe {user-private-url true
                 "new-game" true
                 "update-game" true}
     :publish   {user-private-url true}
     :rpc       {"new-game" true
                 "join-game" true}}))

;;
;; /game/<id>/<user-id>
;;
;; for each player
;;   wgame/private-state-for-player
;;

(defn send-private-game-state! [game]
  (for [username (wgame/players game)
        :let [player-channel (game-private-channel-url (:id game) username)]]
    (wamp/send-event! player-channel
                      (wgame/private-state-for-player game username))))

(defn- send-reset-state! [sess-id]
  (let [username (state/username-by-session-id sess-id)]
    (wamp/send-event! (user-private-channel-url username)
                      {:type :reset-state
                       :state {:games (map wgame/public-game (state/get-all-games))
                               :session-id sess-id
                               :username username}})))

(defn- send-game! [game]
  (wamp/send-event! "new-game" (wgame/public-game game)))

(defn- send-update-game! [game]
  (wamp/send-event! "update-game" (wgame/public-game game)))

(defn- new-game []
  (let [sess-id wamp/*call-sess-id*]
    (let [game (state/add-game! (state/get-user-by-session-id sess-id))]
      (send-game! game)
      (:id game))))

(defn- join-game [game-id team]
  (let [res (state/add-player-to-game! game-id (keyword team) wamp/*call-sess-id*)]
    (if res
      (do
        (log/info "Sending update-game!")
        (send-update-game! (@state/games-by-id game-id)) {})
      {:error "Error joining the game"})))

(defn- make-turn [game-id turn]
  (send-private-game-state!
    (wgame/make-turn 
      (@state/games-by-id game-id)
      (state/username-by-session-id wamp/*call-sess-id*)
      turn)))

(defn- on-subscribe [sess-id topic]
  (log/info (state/username-by-session-id sess-id) " subscribing to " topic)
  (when (.startsWith topic "user")
    (send-reset-state! sess-id))
  true)

(defn wamp-handler
  "Returns an http-kit websocket handler with wamp subprotocol"
  [req]
  (wamp/with-channel-validation req channel (:ws-origins-re (conf))
    (wamp/http-kit-handler channel
      {:on-open        on-open
       :on-close       on-close
       :on-subscribe   {"user/*"  true
                        "new-game" true
                        "update-game" true
                        :on-after on-subscribe}
       :on-call        {"new-game" new-game
                        "join-game" join-game }
       :on-publish     {:on-after on-publish}
       :on-auth        {:secret  auth-secret
                        :permissions auth-permissions}})))
