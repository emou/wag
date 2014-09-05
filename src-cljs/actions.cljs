(ns wag.actions
  (:require [wag.views :as views]
            [wag.wamp-client :as wamp-client]
            [wag.core]
            [wag.log :as log]
            [wag.state]
            [wag.routes :as routes]))

(def WS_URI "ws://localhost:8080/ws")

(defn login []
  {:view views/login})

(defn dashboard []
  {:view views/dashboard})

(defn make-turn [turn]
  (let [game-id (:played-game-id @wag.state/app-state)
        new-turn (assoc turn :from (:username @wag.state/app-state))]
    (log/debug "Making turn " new-turn " for game " game-id)
    (wamp-client/rpc-call (wag.state/get-wamp-session)
                          ["make-turn" game-id new-turn]
                          (fn [res]
                            (log/debug "make-turn returned" res)))))

(defn play-game [game-id]
  (wag.state/set-played-game! game-id)
  {:view views/play-game})

(defn new-game []
  (wamp-client/rpc-call (wag.state/get-wamp-session)
                        ["new-game"]
                        (fn [game-id]
                          (log/debug "new-game returned" game-id)
                          (wag.state/set-played-game! game-id)))
  {:view views/play-game})

(defn choose-game []
  {:view views/choose-game})

(defn join-game [game-id]
  (wag.state/set-joining-game! game-id)
  {:view views/join-game})

(defn join-game-team [game-id team]
  (log/debug "Attempting to join" game-id "/" team)
  (wamp-client/rpc-call (wag.state/get-wamp-session)
                        ["join-game" game-id team]
                        (fn [ret]
                          (log/debug "join-game returned" ret)))
  (wag.state/set-joining-game! nil)
  (play-game game-id))

(defn handle-error [error]
  (letfn [(error-message [error]
           (case (:type error)
             :auth "Wrong username or password. Please try again"
             "Could not connect to the server. Please try again later"))]
    (log/error (error-message error)
      (routes/dispatch! "/login"))))

(defn on-connection [{:keys [error, session, username]}]
  (if error
    (handle-error error)
    (do
      (.subscribe
        session
        (str "user/" username)
        (fn [topic, event]
          (log/debug "Got private event on " topic event)
          (wag.state/handle-event! event)))

      (.subscribe
        session
        "new-game"
        (fn [topic, event]
          (wag.state/handle-new-game! event)))

      (.subscribe
        session
        "update-game"
        (fn [topic, event]
          (wag.state/handle-update-game! event)))

      (wag.state/set-wamp-session! session)

      (routes/dispatch! "/dashboard"))))

(defn attempt-login [username password]
  (do
    (println "Attempting log in as " username)
    (wamp-client/connect WS_URI username password on-connection)))
