(ns wag.websocket
  (:use [wag.config :only [conf]])
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server :as http-kit]
            [clj-wamp.server :as wamp]
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

(defn- on-user-authenticated [sess-id username]
  (state/add-user! sess-id username))

(defn- auth-permissions
  "Returns the permissions for a client session by auth key."
  [sess-id auth-key]

  (let [user-private-url (user-private-channel-url auth-key)]
    (on-user-authenticated sess-id auth-key)
    {:subscribe {user-private-url true}
     :publish   {user-private-url true}
     :rpc       {"new-game" true}}))

(defn- send-initial-state! [sess-id]
  (let [username (state/username-by-session-id sess-id)]
    (wamp/send-event! (user-private-channel-url username)
                      {:type :reset-state
                       :state {:joined-games (state/games-for-user username)
                               :session-id sess-id
                               :username username}})))

; TODO
(defn- send-game! [game-id])

(defn- new-game []
  (let [sess-id wamp/*call-sess-id*]
    (log/info "new-game. games: " @state/games-by-id)
    (let [game-id (state/add-game! (state/get-user-by-session-id sess-id))]
      (send-game! sess-id)
      game-id)))

(defn- on-subscribe [sess-id topic]
  (log/info (state/username-by-session-id sess-id) " subscribing to private channel " topic)
  (when (.startsWith topic "user")
    (send-initial-state! sess-id))
  true)

(defn wamp-handler
  "Returns an http-kit websocket handler with wamp subprotocol"
  [req]
  (wamp/with-channel-validation req channel (:ws-origins-re (conf))
    (wamp/http-kit-handler channel
      {:on-open        on-open
       :on-close       on-close
       :on-subscribe   {"user/*"  true
                        :on-after on-subscribe}
       :on-call        {"new-game"  new-game}
       :on-publish     {:on-after on-publish}
       :on-auth        {:secret  auth-secret
                        :permissions auth-permissions}})))
