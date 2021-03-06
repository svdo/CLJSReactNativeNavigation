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
 {:navigation-button-pressed
  (fn [{:keys [buttonId]}]
    (js/alert (str "Button was pressed: " buttonId)))

  :render
  (fn [{:keys [component-id] :as props}]
    [:> rn/View {:style {:flex-direction "column"
                         :margin 40
                         :align-items "center"
                         :background-color "white"}}
     [content]])})

(defn reload {:dev/after-load true} []
  (.forceUpdate ^js @component-to-update))

(defn init []
  (env/register "App"
                {:topBar {:visible "true"
                          :title {:text "My App"}
                          :rightButtons [{:id "add" :systemItem "add"}]}})

  (-> (rnn/Navigation.events)
      (.registerAppLaunchedListener
       (fn []
         (->> {:root
               {:stack
                {:children [{:component {:name "App"}}]}}}
              (clj->js)
              (rnn/Navigation.setRoot))))))
