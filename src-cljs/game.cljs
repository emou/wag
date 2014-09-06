(ns wag.game
  (:require [wag.log :as log]))

(def PLAYERS_COUNT 4)
(def TEAM_PLAYERS_COUNT 2)
(def VALID_TEAMS #{:team-a :team-b})

(defn player-count [game]
  "Returns the number of players that have joined a game."
  (+ (count (:team-a game)) (count (:team-b game))))

(defn joined-team? [game team username]
  "Returns whether a user has joined a team in a game"
  (some (partial = username) (game team)))

(defn joined? [game my-username]
  "Returns whether a user has joined the game regardless of the team"
  (or (joined-team? game :team-a my-username)
      (joined-team? game :team-b my-username)))

(defn players-needed [game]
  "Return the number of players that need to join the game before it can start"
  (- PLAYERS_COUNT (player-count game)))

(defn team-full? [game team]
  "Checks whether a team is full, i.e. if it is joinable."
  (assert (contains? VALID_TEAMS team) (str "Invalid team " team))
  (>= (count (team game)) TEAM_PLAYERS_COUNT))

(defn- teammates-by-user [game]
  "Returns a map of teammates for a user. The game must be full"
  (assert (= (player-count game) PLAYERS_COUNT))
  {(first (:team-a game)) (second (:team-a game))
   (second (:team-a game)) (first (:team-a game))
   (first (:team-b game)) (second (:team-b game))
   (second (:team-b game)) (first (:team-b game))})

(defn teammate [game user]
  "Returns the teammate of a user"
  ((teammates-by-user game) user))
