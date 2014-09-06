(ns wag.state
  (:require [wag.game :as wgame]))

(defonce games-by-id (ref {}))
(defonce users-by-username (ref {}))
(defonce usernames-by-session-id (ref {}))

(defn- generate-game-id [] (str (java.util.UUID/randomUUID)))

(defn add-game! [{:keys [username]}]
  "Create and add a new game with username as the creator."
  (let [game-id (generate-game-id)
        game (wgame/new-game game-id username)]
    (dosync
      (alter users-by-username update-in [username :game-ids] conj game-id)
      (alter games-by-id assoc game-id game))
    game))

(defn all-games []
  "Return all available games"
  (vals @games-by-id))

(defn add-user! [sess-id username]
  "Create a user unless already exists and add him to the global state."
  (dosync
    (when-not (contains? @users-by-username username)
      (alter users-by-username assoc username
             {:username username
              :game-ids #{}}))
    (alter usernames-by-session-id assoc sess-id username)))

(defn username-by-session-id [sess-id]
  "Given a WAMP session id, return the username"
  (@usernames-by-session-id sess-id))

(defn make-turn! [game-id sess-id turn]
  "Update the game with a new state after making the given turn.
  Returns the modified game state"
  ((dosync
    (alter games-by-id
           assoc
           game-id
           (wgame/make-turn 
             (@games-by-id game-id)
             (username-by-session-id sess-id)
             turn))) game-id))

(defn get-user-by-session-id [sess-id]
  "Given a WAMP session id, return the user"
  (->>
    (@usernames-by-session-id sess-id)
    (@users-by-username)))

(defn add-player-to-game! [game-id team sess-id]
  "Add player associated with sess-id to game"
  ((dosync
     (let [username (username-by-session-id sess-id)]
       (alter users-by-username update-in [username :game-ids] conj game-id)
       (alter games-by-id update-in [game-id]
              (fn [game] (wgame/add-player-to-game game team username)))))
     game-id))

(comment
  ;; repl test area
  (defn- clear-state! []
    (dosync
      (alter games-by-id (identity {}))
      (alter users-by-username (identity {}))
      (alter usernames-by-session-id (identity {}))))
  (clear-state!)
  (add-game! {:username "me"})
  (@state/games-by-id "a10daa8e-2fbf-4a78-ac23-a60780b0fa52")
  (games-for-user "guest")
  (map #(games-by-id %) (get-in @users-by-username [(@usernames-by-session-id "1409818230319-217")])))
