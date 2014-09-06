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
      (alter users-by-username update-in [username :game-ids] conj game-id)
      (alter games-by-id assoc game-id game))
    game))

(defn all-games []
  (vals @games-by-id))

(defn add-user! [sess-id username]
  (dosync
    (when-not (contains? @users-by-username username)
      (alter users-by-username assoc username
             {:username username
              :game-ids #{}}))
    (alter usernames-by-session-id assoc sess-id username)))

(defn username-by-session-id [sess-id]
  (@usernames-by-session-id sess-id))

(defn make-turn! [game-id sess-id turn]
  ((dosync
    (alter games-by-id
           assoc
           game-id
           (wgame/make-turn 
             (@games-by-id game-id)
             (username-by-session-id sess-id)
             turn))) game-id))

(defn get-user-by-session-id [sess-id]
  (->>
    (@usernames-by-session-id sess-id)
    (@users-by-username)))

(defn add-player-to-game! [game-id team sess-id]
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
