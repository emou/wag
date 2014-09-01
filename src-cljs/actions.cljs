(ns wag.actions
  (:require [wag.views :as views]))

(defn login []
  {:state {}
   :template views/login})

(defn dashboard []
  {:state {}
   :template views/dashboard})

(defn new-game []
  {:state {}
   :template views/new-game})

(defn join-game []
  {:state {}
   :template views/join-game})
