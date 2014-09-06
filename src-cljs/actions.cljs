(ns wag.actions
  (:require [wag.views :as views]
            [wag.wamp-client :as wamp-client]
            [wag.core]
            [wag.log :as log]
            [wag.state]
            [wag.routes :as routes]))

(def WS_URI "ws://localhost:8080/ws")

(defn login []
  "Show the login screen"
  {:view views/login})

(defn dashboard []
  "Show the home/dashboard screen"
  {:view views/dashboard})

(defn make-turn [turn]
  "Make a new turn in the game"
  (let [game-id (:played-game-id @wag.state/app-state)
        new-turn (assoc turn :from (:username @wag.state/app-state))]
    (log/debug "Making turn " new-turn " for game " game-id)
    (wamp-client/rpc-call (wag.state/get-wamp-session)
                          ["make-turn" game-id new-turn]
                          (fn [res]
                            (log/debug "make-turn returned" res)))))

(defn play-game [game-id]
  "Start playing a game with `game-id`. It must have been previously loaded"
  (wag.state/set-played-game! game-id)
  {:view views/play-game})

(defn new-game []
  "Start a new game and show the play screen for it"
  (wamp-client/rpc-call (wag.state/get-wamp-session)
                        ["new-game"]
                        (fn [game-id]
                          (log/debug "new-game returned" game-id)
                          (wag.state/set-played-game! game-id)))
  {:view views/play-game})

(defn choose-game []
  "Choose an existing game to join"
  {:view views/choose-game})

(defn join-game [game-id]
  "Show the screen for joining an existing game"
  (wag.state/set-joining-game! game-id)
  {:view views/join-game})

(defn join-game-team [game-id team]
  "Join a team in a game. This actually joins the game and shows its play screen"
  (log/debug "Attempting to join" game-id "/" team)
  (wamp-client/rpc-call (wag.state/get-wamp-session)
                        ["join-game" game-id team]
                        (fn [ret]
                          (log/debug "join-game returned" ret)))
  (wag.state/set-joining-game! nil)
  (play-game game-id))

(defn- handle-error [error]
  (letfn [(error-message [error]
           (case (:type error)
             :auth "Wrong username or password. Please try again"
             "Could not connect to the server. Please try again later"))]
    (log/error (error-message error)
      (routes/dispatch! "/login"))))

(defn- handle-connection-success [session username]
  (wag.state/set-wamp-session! session)

  (wamp-client/subscribe
    session
    (str "user/" username)
    (fn [topic, event]
      (log/debug "Got private event on " topic event)
      (wag.state/handle-event! event)))

  (wamp-client/subscribe
    session
    "new-game"
    (fn [topic, event]
      (wag.state/handle-new-game! event)))

  (wamp-client/subscribe
    session
    "update-game"
    (fn [topic, event]
      (wag.state/handle-update-game! event)))

  (routes/dispatch! "/dashboard"))

(defn- on-connection [{:keys [error, session, username]}]
  (if error
    (handle-error error)
    (handle-connection-success session username)))

(defn attempt-login [username password]
  "Attempt logging in to the WAMP server with the given username and password."
  (wamp-client/connect WS_URI username password on-connection))
