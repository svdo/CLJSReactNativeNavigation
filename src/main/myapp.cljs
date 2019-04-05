(ns myapp
  (:require [reagent.core :as r :refer [atom]]
            ["react-native" :as rn :refer [AppRegistry]]
            ["react-native-navigation" :as rnn]
            [env]))

(defonce component-to-update (atom nil))

(defn content []
  [:> rn/Text {:style {:font-size 30
                       :font-weight "100"
                       :margin-bottom 20
                       :text-align "center"}}
   "Hi Shadow!"])


(env/add-screen
 "App"
 {:render
  (fn [{:keys [component-id] :as props}]
    [:> rn/View {:style {:flex-direction "column"
                         :margin 40
                         :align-items "center"
                         :background-color "white"}}
     [content]])})

(defn reload {:dev/after-load true} []
  (.forceUpdate ^js @component-to-update))

(defn init []
  (env/register "App")

  (-> (rnn/Navigation.events)
      (.registerAppLaunchedListener
       (fn []
         (->> {:root
               {:stack
                {:children [{:component {:name "App"}}]
                 :options {:topBar {:visible "true"
                                    :title {:text "My App"}
                                    :rightButtons [{:id "add" :systemItem "add"}]}}}}}
              (clj->js)
              (rnn/Navigation.setRoot))))))
