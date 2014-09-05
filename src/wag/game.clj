(ns wag.game
  (:require [clojure.tools.logging :as log]))

(def GAME_PLAYERS_PER_TEAM 2)
(def GAME_PLAYERS 4)

(defn player-count [game]
  (+ (count (:team-a game))
     (count (:team-b game))))

(defn game-full? [game]
  (= (player-count game) GAME_PLAYERS))

(defn- new-private-state []
  {:next-turn nil
   :teller nil
   :knower nil
   :turn-history []})

(defn new-game [id creator]
  {:id id
   :creator creator
   :team-a #{creator}
   :team-b #{}
   :private-state (new-private-state)})

(defn- create-turn [turn-type from]
  { :type turn-type 
    :from from })

(defn- first-turn [game]
  (create-turn 
    :tell-secret
    (get-in game [:private-state :teller])))

(defn- secret-teller [game]
  (first (game :team-a)))

(defn- secret-knower [game]
  (first (game :team-b)))

(defn- initial-roles [game]
  (update-in game [:private-state] merge
             {:teller (first (game :team-a))
              :teller-mate (second (game :team-a))
              :knower (first (game :team-b))
              :knower-mate (second (game :team-b)) }))

(defn- initial-next-turn [game]
  (update-in game [:private-state] merge
             {:next-turn (first-turn game)}))

(defn- start-game [game]
  (->
    game
    (initial-roles)
    (initial-next-turn)))

(defn add-player-to-game [game team username]
  (when (and
          (< (count (team game)) GAME_PLAYERS_PER_TEAM))
    (let [ngame
          (update-in game [team] conj username)]
      (if (game-full? ngame)
        (start-game ngame)
        ngame))))

(defn players [game]
  (concat (:team-a game) (:team-b game)))

(defn- winner [game]
  (get-in game [:private-state :winner]))

(defn- teller [game]
  (get-in game [:private-state :teller]))

(defn- teller-mate [game]
  (get-in game [:private-state :teller-mate]))

(defn- knower [game]
  (get-in game [:private-state :knower]))

(defn- knower-mate [game]
  (get-in game [:private-state :knower-mate]))

(defn finished? [game]
  (boolean (winner game)))

(defn private-state-for-player [game player]
  (let [private-state (:private-state game)]
    (if (or (finished? game)
            (= player (teller game))
            (= player (knower game)))
      private-state
      (dissoc private-state :secret))))

(defn public-game [game]
  (dissoc game :private-state))

(defn- next-hint-from [game last-guess-from]
  (if (= last-guess-from (teller-mate game))
    (knower game)
    (teller game)))

(defn- next-guess-from [game last-hint-from]
  (if (= last-hint-from (teller game))
    (teller-mate game)
    (knower-mate game)))

(defn- handle-tell-secret [game turn]
  (do
    (-> game
      (assoc-in [:private-state :next-turn]
                {:type :hint
                 :from (knower game)})

      (assoc-in [:private-state :secret]
                (:value turn)))))

(defn- handle-hint [game turn]
  (assoc-in game
            [:private-state :next-turn]
            {:type :guess
             :from (next-guess-from game (:from turn)) }))

(defn- team-of-player [game player]
  (if (contains? (:team-a game) player)
    :team-a
    :team-b))

(defn- game-won [game player]
  (-> game
    (assoc-in [:private-state :next-turn] nil)
    (assoc-in [:private-state :winner] (team-of-player game player))))

(defn- handle-guess [game turn]
  (if (= (get-in game [:private-state :secret]) (:value turn))
    (game-won game (:from turn))
    (assoc-in game
              [:private-state :next-turn]
              {:type :hint
               :from (next-hint-from game (:from turn)) })))

(defn- public-turn [turn]
  (if (= :tell-secret (:type turn))
    (dissoc turn :value)
    turn))

(defn make-turn [game username turn]
  (let [expected-next-turn (get-in game [:private-state :next-turn])
        expected-keys [:type :from]]
    (assert (.equals (select-keys turn expected-keys)
                     (select-keys expected-next-turn expected-keys))
            (str "expected " expected-next-turn ", got " turn))
    (->
      (:type turn)
      (case
        :tell-secret (handle-tell-secret game turn)
        :hint (handle-hint game turn)
        :guess (handle-guess game turn)
        (assert false (str "Unhandled turn  type " (:type turn))))
      (update-in [:private-state :turn-history] conj (public-turn turn)))))
