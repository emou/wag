(ns wag.websocket
  (:use [wag.config :only [conf]])
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server :as http-kit]
            [clj-wamp.server :as wamp]))

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

(defn- generate-game-id [] (str (java.util.UUID/randomUUID)))

(def games-by-id (atom {}))
(def users-by-username (atom {}))
(def usernames-by-session-id (atom {}))

(defn- add-user! [sess-id username]
  (when-not (contains? @users-by-username username)
    (swap! users-by-username assoc username
           {:username username
            :game-ids #{}}))
  (swap! usernames-by-session-id assoc sess-id username))

(defn- get-user-by-session-id [sess-id]
  (->>
    (@usernames-by-session-id sess-id)
    (@users-by-username)))

(defn- add-game! [{:keys [username]}]
  (let [game-id (generate-game-id)]
    (swap! games-by-id assoc game-id
           {:creator username
            :players [username]})
    (swap! users-by-username update-in [username :game-ids] conj game-id)))

(defn- games-for-user [username]
  (map #(games-by-id %) (get-in users-by-username [username :game-ids])))

(defn- on-user-authenticated [sess-id username]
  (add-user! sess-id username))

(defn- user-private-channel-url [username]
  (str "user/" username))

(defn- auth-permissions
  "Returns the permissions for a client session by auth key."
  [sess-id auth-key]

  (let [user-private-url (user-private-channel-url auth-key)]
    (on-user-authenticated sess-id auth-key)
    {:subscribe {user-private-url true}
     :publish   {user-private-url true}
     :rpc       {(rpc-url "new-game")  true}}))

(defn- on-subscribe [sess-id topic]
  (log/info (@usernames-by-session-id sess-id) " subscribing to private channel " topic)
  (when (.startsWith topic "user")
    (wamp/send-event! topic {:type :games
                             :games (games-for-user (@usernames-by-session-id sess-id))}))
  true)

(defn- new-game []
  (let [sess-id wamp/*call-sess-id*]
    (log/info "new-game. games: " @games)
    (add-game! (get-user-by-session-id sess-id))))

(defn wamp-handler
  "Returns an http-kit websocket handler with wamp subprotocol"
  [req]
  (wamp/with-channel-validation req channel (:ws-origins-re (conf))
    (wamp/http-kit-handler channel
      {:on-open        on-open
       :on-close       on-close
       :on-subscribe   {"user/*"  true
                        :on-after on-subscribe}
       :on-call        {(rpc-url "new-game")  new-game}
       :on-publish     {:on-after         on-publish}
       :on-auth        {:secret           auth-secret
                        :permissions      auth-permissions}})))
