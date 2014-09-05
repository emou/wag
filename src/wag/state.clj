(ns wag.state
  (:require [wag.game :as wgame]))

(defonce games-by-id (ref {}))
(defonce users-by-username (ref {}))
(defonce usernames-by-session-id (ref {}))

(defn- generate-game-id [] (str (java.util.UUID/randomUUID)))

(defn add-game! [{:keys [username]}]
  (let [game-id (generate-game-id)
        game (wgame/new-game game-id username)]
    (dosync
      (alter games-by-id assoc game-id game)
      (alter users-by-username update-in [username :game-ids] conj game-id))
    game))

(defn get-all-games []
  (vals @games-by-id))

(defn add-user! [sess-id username]
  (dosync
    (when-not (contains? @users-by-username username)
      (alter users-by-username assoc username
             {:username username
              :game-ids #{}}))
    (alter usernames-by-session-id assoc sess-id username)))

(defn get-user-by-session-id [sess-id]
  (->>
    (@usernames-by-session-id sess-id)
    (@users-by-username)))

(defn username-by-session-id [sess-id]
  (@usernames-by-session-id sess-id))

(defn add-player-to-game! [game-id team sess-id]
  (let [result (dosync
                 (alter games-by-id update-in [game-id]
                        (fn [game] (wgame/add-player-to-game game team (username-by-session-id sess-id)))))]
    result))

(comment
  ;; repl test area
  (defn- clear-state! []
    (dosync
      (alter games-by-id (identity {}))
      (alter users-by-username (identity {}))
      (alter usernames-by-session-id (identity {}))))
  (clear-state!)
  (games-for-user "guest")
  (map #(games-by-id %) (get-in @users-by-username [(@usernames-by-session-id "1409818230319-217")])))
