(ns wag.actions
  (:require [wag.views :as views]))

(defn login []
  {:template views/login})

(defn dashboard []
  {:template views/dashboard})

(defn new-game []
  {:template views/new-game})

(defn join-game []
  {:template views/join-game})
