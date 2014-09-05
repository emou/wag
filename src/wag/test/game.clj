(ns wag.test.game
  (:require [wag.game :as wgame])
  (:use clojure.test))

(def game (atom nil))

(defn setup-game [f]
  (reset! game (wgame/new-game "1" "a-first"))
  (f))

(defn fill-game [game]
  (->
    game
    (wgame/add-player-to-game :team-a "a-second")
    (wgame/add-player-to-game :team-b "b-first")
    (wgame/add-player-to-game :team-b "b-second")))

(defn tell-secret [game]
  (wgame/make-turn game "a-first" {:type :tell-secret
                                   :from "a-first"
                                   :value "cow"}))

(defn hint-first [game]
  (wgame/make-turn game "b-first" {:type :hint
                                   :from "b-first"
                                   :value "milk"}))

(defn make-guess [game player guess]
  (wgame/make-turn game "b-first" {:type :guess
                                   :from player
                                   :value guess}))

(deftest gametest
  (use-fixtures :each setup-game)
  (testing "Game"
    (testing "new-game"
      (is (= (:team-a @game) #{"a-first"}))
      (is (= (:team-b @game) #{}))
      (is (= (:private-state @game)
              {:next-turn nil
               :teller nil
               :knower nil
               :turn-history []})))
    (testing "add-player-to-game"
      (is (= (:team-a (wgame/add-player-to-game @game :team-a "a-second")
                      #{"a-first" "a-second"})))
      (is (= (get-in (fill-game @game) [:private-state :teller]) "a-first"))
      (is (= (get-in (fill-game @game) [:private-state :teller-mate]) "a-second"))
      (is (= (get-in (fill-game @game) [:private-state :knower]) "b-first"))
      (is (= (get-in (fill-game @game) [:private-state :knower-mate]) "b-second"))
      (is (= (get-in (fill-game @game) [:private-state :next-turn])
             {:type :tell-secret, :from "a-first" }))

      (is (= (get-in (tell-secret (fill-game @game)) [:private-state :next-turn])
             {:type :hint
              :from "b-first"}))

      (is (= (get-in (tell-secret (fill-game @game)) [:private-state :secret])
             "cow"))

      (is (= (get-in (tell-secret (fill-game @game)) [:private-state :turn-history])
             [{:type :tell-secret
               :from "a-first"}]))

      (is (= (get-in (hint-first (tell-secret (fill-game @game)))
                     [:private-state :next-turn])
             {:type :guess
              :from "b-second"}))

      (is (= (get-in (make-guess (hint-first (tell-secret (fill-game @game)))
                                 "b-second" "cheese")
                     [:private-state :next-turn])
             {:type :hint
              :from "a-first"}))

      (is (= (get-in (make-guess (hint-first (tell-secret (fill-game @game)))
                                 "b-second" "cow")
                     [:private-state :next-turn]) nil))

      (is (= (get-in (make-guess (hint-first (tell-secret (fill-game @game)))
                                 "b-second" "cow")
                     [:private-state :winner]) :team-b)))))

(run-tests)
