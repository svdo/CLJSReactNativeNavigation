(ns myapp
  (:require [reagent.core :as r :refer [atom]]
            ["react-native" :as rn :refer [AppRegistry]]))

(defonce component-to-update (atom nil))

(defn content []
  [:> rn/Text {:style {:font-size 30
                       :font-weight "100"
                       :margin-bottom 20
                       :text-align "center"}}
   "Hi Shadow!"])

(defn app-root []
  [:> rn/View {:style {:flex-direction "column"
                       :margin 40
                       :align-items "center"
                       :background-color "white"}}
   [content]])

(def updatable-app-root
  (with-meta app-root
    {:component-did-mount
     (fn [] (this-as ^js this
                     (reset! component-to-update this)))}))

(defn reload {:dev/after-load true} []
  (.forceUpdate ^js @component-to-update))

(defn init []
  (.registerComponent AppRegistry
                      "CLJSReactNativeNavigation"
                      #(r/reactify-component updatable-app-root)))
