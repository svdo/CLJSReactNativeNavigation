(ns myapp
  (:require [reagent.core :as r :refer [atom]]
            ["react-native" :as rn :refer [AppRegistry]]))

(defn app-root []
  [:> rn/View {:style {:flex-direction "column"
                       :margin 40
                       :align-items "center"
                       :background-color "white"}}

   [:> rn/Text {:style {:font-size 30
                        :font-weight "100"
                        :margin-bottom 20
                        :text-align "center"}}

    "Hi Shadow!"]])

(defn init []
  (.registerComponent AppRegistry
                      "CLJSReactNativeNavigation"
                      #(r/reactify-component app-root)))
