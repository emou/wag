(ns wag.game
  (:require [wag.log :as log]))

(def PLAYERS_COUNT 4)
(def TEAM_PLAYERS_COUNT 2)
(def VALID_TEAMS #{:team-a :team-b})

(defn player-count [game]
  (+ (count (:team-a game)) (count (:team-b game))))

(defn other-players [game my-username]
  (remove #(= (:username %) my-username) (:players game)))

(defn joined-team? [game team username]
  (some (partial = username) (game team)))

(defn joined? [game my-username]
  (or (joined-team? game :team-a my-username)
      (joined-team? game :team-b my-username)))

(defn players-needed [game]
  (- PLAYERS_COUNT (player-count game)))

(defn team-full? [game team]
  (assert (contains? VALID_TEAMS team) (str "Invalid team " team))
  (>= (count (team game)) TEAM_PLAYERS_COUNT))

(defn teammates-by-user [game]
  {(first (:team-a game)) (second (:team-a game))
   (second (:team-a game)) (first (:team-a game))
   (first (:team-b game)) (second (:team-b game))
   (second (:team-b game)) (first (:team-b game))})

(defn teammate [game user]
  (log/debug "game" game)
  (log/debug "teammates-by-user" (teammates-by-user game))
  ((teammates-by-user game) user))
