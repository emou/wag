(ns wag.actions
  (:require [wag.views :as views]
            [wag.wamp-client :as wamp-client]
            [wag.core]
            [wag.log :as log]))

(enable-console-print!)

(defn login []
  {:view views/login})

(defn dashboard []
  {:view views/dashboard})

(defn new-game []
  (wamp-client/rpc-call (wag.state/get-wamp-session)
                        "new-game"
                        (fn [result]
                          (wag.state/set-played-game! result)))
  {:view views/play-game})

(defn join-game []
  {:view views/join-game})

(defn play-game [game-id]
  (log/info "play-game !" game-id)
  {:view views/play-game})
