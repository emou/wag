(ns wag.websocket
  (:use [wag.config :only [conf]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [org.httpkit.server :as http-kit]
            [clj-wamp.server :as wamp]
            [wag.game :as wgame]
            [wag.state :as state]))

(defn- on-open [sess-id]
  (log/info "WAMP client connected [" sess-id "]"))

(defn- on-close [sess-id status]
  (log/info "WAMP client disconnected [" sess-id "] " status))

(defn- on-publish [sess-id topic event exclude include]
  (log/info "WAMP publish:" sess-id topic event exclude include))

;; TODO: Implement database-backed authentication
;; Currently all users can login using the hardcoded password below.
(defn- auth-secret [sess-id auth-key extra]
  "1")

(defn- user-private-channel-url [username]
  (str "user/" username))

(defn- user-private-game-url [game-id username]
  (str "game/" game-id "/" username))

(defn- on-user-authenticated [sess-id username]
  (state/add-user! sess-id username))

(defn- auth-permissions
  "Returns the permissions for a client session by auth key."
  [sess-id auth-key]

  (on-user-authenticated sess-id auth-key)

  (let [user-private-url (user-private-channel-url auth-key)
        user (state/get-user-by-session-id sess-id)
        username (:username user)
        game-private-url #(user-private-game-url % username)]
    {:subscribe (into {user-private-url true
                       "new-game" true
                       "update-game" true}
                      (for [game-id (:game-ids user)] ;; No support for pattern matching..
                        [(game-private-url game-id) true]))

     :publish   {user-private-url true}
     :rpc       {"new-game" true
                 "join-game" true
                 "make-turn" true}}))

(defn send-private-game-state-for-user! [game username]
  (let [game-id (:id game)
        game-channel (user-private-game-url game-id username)]
    (wamp/send-event! game-channel
                      {:game-id game-id
                       :private-state (wgame/private-state-for-player game username)})))

(defn send-private-game-state! [game]
  (doseq [username (wgame/players game)]
    (send-private-game-state-for-user! game username)))

(defn- send-reset-state! [sess-id]
  (let [username (state/username-by-session-id sess-id)]
    (wamp/send-event! (user-private-channel-url username)
                      {:type :reset-state
                       :state {:games (map wgame/public-game (state/all-games))
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
  (let [new-game (state/add-player-to-game!
                   game-id
                   (keyword team)
                   wamp/*call-sess-id*)]
    (if new-game
      (do
        (send-update-game! new-game)
        (send-private-game-state! (@state/games-by-id game-id))
        true)
      {:error "Error joining the game"})))

(defn- make-turn [game-id raw-turn]
  (let [turn (update-in (keywordize-keys raw-turn) [:type] keyword)
        new-game (state/make-turn! game-id wamp/*call-sess-id* turn)]
    (send-private-game-state! new-game)))

(defn- on-subscribe [sess-id topic]
  (log/info (state/username-by-session-id sess-id) " subscribing to " topic)

  (when (.startsWith topic "user/")
    (send-reset-state! sess-id))

  (when (.startsWith topic "game/")
    (let [game-id (second (string/split topic #"/"))
          username (state/username-by-session-id sess-id)]
      (send-private-game-state-for-user! (@state/games-by-id game-id) username)))
  true)

(defn wamp-handler
  "Returns an http-kit websocket handler with wamp subprotocol"
  [req]
  (wamp/with-channel-validation req channel (:ws-origins-re (conf))
    (wamp/http-kit-handler channel
      {:on-open        on-open
       :on-close       on-close
       :on-subscribe   {"user/*"  true
                        "game/*" true
                        "new-game" true
                        "update-game" true
                        :on-after on-subscribe}
       :on-call        {"new-game" new-game
                        "join-game" join-game
                        "make-turn" make-turn}
       :on-publish     {:on-after on-publish}
       :on-auth        {:secret  auth-secret
                        :permissions auth-permissions}})))
