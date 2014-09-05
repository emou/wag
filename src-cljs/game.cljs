(ns wag.game
  (:require [wag.log :as log]))

(def PLAYERS_COUNT 4)
(def TEAM_PLAYERS_COUNT 2)
(def VALID_TEAMS #{:team-a :team-b})

(defn player-count [game]
  (+ (count (:team-a game)) (count (:team-b game))))

(defn other-players [game my-username]
  (remove #(= (:username %) my-username) (:players game)))

(defn joined? [game my-username]
  (letfn [(joined-team? [team]
            (some (partial = my-username) (team game)))]
    (or (joined-team? :team-a) (joined-team? :team-b))))

(defn players-needed [game]
  (- PLAYERS_COUNT (player-count game)))

(defn team-full? [game team]
  (assert (contains? VALID_TEAMS team) (str "Invalid team " team))
  (>= (count (team game)) TEAM_PLAYERS_COUNT))
