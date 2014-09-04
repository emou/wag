(ns wag.actions
  (:require [wag.views :as views]
            [wag.wamp-client :as wamp-client]
            [wag.core]
            [wag.log :as log]
            [wag.state]))

(defn login []
  {:view views/login})

(defn dashboard []
  {:view views/dashboard})

(defn new-game []
  (wamp-client/rpc-call (wag.state/get-wamp-session)
                        ["new-game"]
                        (fn [game-id]
                          (wag.state/set-played-game! (keyword game-id))))
  {:view views/play-game})

(defn choose-game []
  {:view views/choose-game})

(defn join-game [game-id]
  (wamp-client/rpc-call (wag.state/get-wamp-session)
                        ["join-game" game-id]
                        (fn [ret]
                          (log/debug "join-game returned " ret)))
  (wag.state/set-joining-game! game-id)
  {:view views/join-game})

(defn play-game [game-id]
  (wag.state/set-played-game! game-id)
  {:view views/play-game})
