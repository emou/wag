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
                        "new-game"
                        (fn [result]
                          (wag.state/set-played-game! result)))
  {:view views/play-game})

(defn choose-game []
  {:view views/choose-game})

(defn join-game [game-id]
  ;; TODO: Pass local state to om component?
  (wag.state/set-joining-game! game-id)
  {:view views/join-game})

(defn play-game [game-id]
  (wag.state/set-played-game! game-id)
  {:view views/play-game})
