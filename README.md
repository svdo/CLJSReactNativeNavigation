# React Native Navigation and ClojureScript

*A beginner's guide to realizing proper navigation in a React Native application
using ClojureScript and shadow-cljs*

That's a big bunch of words. Let me briefly introduce them to you, so that I can
then dive deeper into some of them. "Beginner" in this case means that, a week
before I started doing this, I had practically zero experience with
Clojure(Script) and React Native. I do have quite a bit of experience using
React, and also writing native iOS applications (Swift/Objective-C).

"Guide" means that I'm going to explain to you how I accomplished my goal.
You'll have to interpret it and follow along if you want to do the same. Future
versions of any of the tools may break this guide.

[React Native Navigation][wix-rnn] is a framework for [React Native][rn]. Which is a
framework for building cross-platform apps for iOS and Android using JavaScript,
but you probably already knew that part. [ClojureScript][cljs] is a functional
programming language. It's a variant of Clojure that compiles to JavaScript.
"Proper navigation" is, admittedly, a bit subjective; I'll explain this part in
more detail below. Finally, [shadow-cljs][shadow-cljs] is the toolchain I used
 as an easy way to integrate a bunch of ClojureScript code into an originally
Javascript-based project.

A summary of this guide is:

1. Setup a new React Native project (using `react-native` cli).
2. Add shadow-cljs to be able to use ClojureScript in that project.
3. Add and setup `react-native-navigation` dependency.
4. Create a bit of infrastructure on the ClojureScript side to be able to use
   the React Native Navigation APIs while using shadow-cljs' hot reloading.

So let's get started. I executed all these steps exactly as I'm describing here,
the result of which you can find in [my GitHub repo][github]. Commit messages
include all details about the commands I used. If you want to read my
rant about wanting to use native navigation, keep reading till the very end of
this post :)

> Please note that this post _does not_ include all the code that you need
> to make it work, that would be too much for a web page. Please refer to
> [the final solution in my GitHub repo][github] if you want to reproduce all of it.

# 1. Create React Native Project

This step is very easy. First, if you haven't got it yet, install
`react-native-cli`:

``` bash
npm install -g react-native-cli
```

Then create a new project:

``` bash
react-native init CLJSReactNativeNavigation
```

So far so good, nothing new here, let's move on.

# 2. Add shadow-cljs

We're going to use the standalone version of shadow-cljs,
[as recommended][build-tool]. So we add the npm dependency for shadow-cljs:

``` bash
yarn add --dev shadow-cljs
```

Now we need to configure shadow-cljs a bit. We create a file called
`shadow-cljs.edn` and give it the following content:

``` clojure
{:source-paths
 ["src/main"                     ;; production code
  "src/test"]                    ;; yes we're going to add tests as well!

 :dependencies
 [[reagent "0.8.1"]]

 :builds
 {:myapp                         ;; the target definition
  {:target :react-native         ;; the target type
   :init-fn myapp/init           ;; react native's entry point
   :output-dir "build"}}}        ;; where to put the built JS
```

This is telling shadow-cljs that there is a target called "myapp", that
it's a React Native target, that the entry point of our app is the function
`init` in the `myapp` namespace, and that the output should be written to the
folder `build`.

Now let's create that `myapp` namespace. In `src/main` create a file called
`myapp.cljs` with the following contents:

``` clojure
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
```

Time to check this out! Run the following commands in your terminal:

``` bash
yarn shadow-cljs compile myapp
yarn react-native run-ios
```

Tada!

<img src="https://040code.github.io/assets/2019-04-02-rnn-clojurescript/Hi%20Shadow%21.png" height="50%" width="50%"  alt="Tada!" style="margin: 0 auto;" />

## Hot Reloading

When using shadow-cljs, you also get its variant of hot reloading. You don't
need to use the developer menu to enable it, but you do need to add a bit of
code to make it work: (a) you need to had a function that performs the reload,
and (b) you need to enable hot reloading in the config.

### Add reload function

Let me just give you the code of `myapp.cljs` and then explain what's going on.

``` clojure
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
```

This is what it does:

* The text content ("Hi Shadow!") is extracted into a separate component
  (`content`), because the call [`forceUpdate`][forceUpdate] that we'll use
  updates everything _below_ the application root component, not the root
  component itself.
* The app-root component is annotated with a `component-did-mount` handler
  (`updatable-app-root`). This handler stores the actual JavaScript object
  that represents the root component into the atom `component-to-update`.
* A function `reload` is added, which takes the value of that atom and calls
  the method [`forceUpdate`][forceUpdate] on it.

One final step remains: enabling hot reloading in `shadow-cljs.edn`. It's new
content is:

``` clojure
{:source-paths
 ["src/main"                     ;; production code
  "src/test"]                    ;; yes we're going to add tests as well!

 :dependencies
 [[reagent "0.8.1"]]

 :builds
 {:myapp                         ;; the target definition
  {:target :react-native         ;; the target type
   :init-fn myapp/init           ;; react native's entry point
   :output-dir "build"           ;; where to put the built JS
   :devtools {:autoload true}}}} ;; enables hot-reloading
```

If you reload the app, changing the text in the `content` component should
cause the app to automatically update!

# 3. Add React Native Navigation

This bit is a somewhat tedious I'm afraid. You'll have to go through [the
instructions to setup React Native Navigation][rnn-installing]. Add the npm
dependency, update your Xcode project, update iOS source code, update Android
build files, and update Android source code. Tip for Android part: Please don't
blindly copy-paste. Some of the instructions refer are not up to date, some
parts are not really needed, etc. If you want you can have a look at
[how I did it][github].

Obviously you'll skip the last step of the instructions, namely the part where
the JavaScript code is updated to use React Native Navigation. We'll address
that in ClojureScript next.

# 4. Integrate and Wrap

Ok, roll up your sleeves, because here comes the interesting part.

## Wrapper functionality

In JavaScript we would need to do something like this:

``` javascript
import { Navigation } from 'react-native-navigation'
Navigation.registerComponent('navigation.playground.WelcomeScreen', () => App)
Navigation.events().registerAppLaunchedListener(() => {
  Navigation.setRoot({
    root: {
      component: {
        name: 'navigation.playground.WelcomeScreen'
      }
    }
  })
})
```

Obviously in ClojureScript we need to do something similar. There's a problem
though: we need a handle to the actual JavaScript component in order to call
`forceUpdate` on it (for hot reloading). React Native Navigation has made the
design choice that it creates new root components for screens that you push
on the navigation stack. So also for those components we need a handle and
call `forceUpdate`. We accomplish this by not registering the component itself
with `Navigation`, but a wrapper of that component.

This causes another problem though. React Native Navigation gives components
that you register a `componentId`. It uses this for its internal registration
so that it can make navigation work. For example, when you push a new screen
onto the navigation stack, it uses the `componentId` to find the screen from
which you are pushing. The problem is that we registered the _wrapper_, but
we're navigating from the _wrapped component_. Which does not have a
`componentId`, because we never registered it with `Navigation`. Solution:
make the wrapper in such a way that it passes its `componentId` on to the
wrapped component.

But there is more! React Native Navigation defines some additional life cycle
methods, such as `navigationButtonPressed`. And for that to work, you need
to call [`Navigation.bindComponent`][navBtnPressed]. So our wrapper also calls
`bindComponent` and forwards `navigationButtonPressed`. Forwarding other
life cycle methods is left as an exercise for the reader.

Here's the main code for the wrapper ([full version][wrapper]):

``` clojure
;; current namespace is `env`

(defonce id-seq-ref (atom 0))
(defonce mounted-ref (atom {}))
(defonce screens-ref (atom {}))

(defn register [key]
  (let [get-props
        (fn [this]
          {::key key
           ::id (-> this .-state .-id)
           :component-id (-> this .-props .-componentId)})

        wrapper
        (crc #js                    ;; crc is create-react-class
              {:displayName
               (str key "Wrapper")

               :getInitialState
               (let [id (swap! id-seq-ref inc)]
                 (fn [] #js {:key key
                             :id id}))

               :componentDidMount
               (fn []
                 (this-as
                  ^js this

                  (bind-component this)
                  (swap! mounted-ref
                         assoc-in [key (-> this .-state .-id)] this)))

               :componentWillUnmount
               (fn []
                 (this-as
                  ^js this

                  (swap! mounted-ref update key dissoc (-> this .-state .-id))))


               ;; FIXME: forward other lifecycles the same way
               :navigationButtonPressed
               (fn []
                 (this-as
                  ^js this

                  (let [{:keys [navigation-button-pressed]}
                        (get @screens-ref key)

                        props
                        (get-props this)]

                    (js/console.log "navigationButtonPressed"
                                    key
                                    (boolean navigation-button-pressed)
                                    (pr-str props))
                    (when navigation-button-pressed
                      (navigation-button-pressed props)))))

               :componentDidAppear
               (fn []
                 (this-as
                  ^js this

                  (js/console.log "componentDidAppear" key)))

               :componentDidDisappear
               (fn []
                 (this-as
                  ^js this

                  (js/console.log "componentDidDisappear" key)))

               :render
               (fn []
                 (this-as
                  ^js this

                  (let [{:keys [render]}
                        (get @screens-ref key)

                        props
                        (get-props this)]

                    (js/console.log "render" key (pr-str props))
                    (-> (render props)
                        (r/as-element)))))})]

    (register-component key (fn [] wrapper))))
```

This stores the mounted components in `mounted-ref`, which we can then use
for the hot reloading:

``` clojure
(defn reload {:dev/after-load true} []
  (doseq [[key instances] @mounted-ref
          [id inst] instances]
    (js/console.log "forceUpdate" key id)
    (.forceUpdate ^js inst)))
```

The `register` method uses an atom `screens-ref` to forward life cycle methods,
so we need to provide a function for screens to add themselves:

``` clojure
(defn add-screen [key screen-def]
  (swap! screens-ref assoc key screen-def))
```

## Using it

Initially, in `myapp/init` we called React Native's `registerComponent`. Now
we call our `env/register` instead. We could just call `(env/register "App")`,
but we want to pass some options for the navigation bar.

Furthermore we need to call the `Navigation.events().registerAppLaunchedListener`
JavaScript function to set the navigation root for our app.

The `init` function is now:

``` clojure
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
```

# Credits

That's it! Done! Just four simple, easy, almost trivial steps! Well maybe not
so trivial. I guess a ClojureScript beginner couldn't have come up with this.
Well, in fact I _am_ a ClojureScript beginner and I _did not_ come up with this
solution myself. All the credits go to [Thomas Heller][thheller], the author of
[shadow-cljs][shadow-cljs]. He has been amazing in his support by answering
all of my beginner-level questions, and then he ended up conjuring this
solution and committing it to my repository. He actually spent hours on this
I believe, and that level of support from a community is truly awesome
(and rare). He doesn't seem to be advertising it very much, but you can become
his [patreon][patreon].

# Why React Native Navigation

What's special about React Native Navigation is that it is implemented using the
real platform native components, specifically
[`UINavigationController`][uinavigationcontroller] on
iOS. I seem to be at odds with most of the rest of the world on this, but I
happen to think that it is very important to present the user with an
experience that is (as much as possible) identical to that of native apps. You
shouldn't be able to tell from the user experience whether the app was written
using native technology or cross platform technology. Not even when you update
your OS to a new major version. So if my app uses a navigation stack, it has to
be the native one. Maybe I'm more sensitive to this then others, but I get
really upset by apps that don't support the normal gesture for going back up the
navigation stack. I also get annoyed when the animation that is used while going
back is slightly non-standard. Using the native components is the only way to
accomplish that. Other components can come close, but not close enough for me.

### Versions

Like I said in the beginning, future versions of any of the tools may break
this guide. So it is only fair to mention which versions I was using for this:

| Tool                    | Version                    |
|-------------------------|----------------------------|
| node                    | 11.10.1                    |
| npm                     | 6.5.0-next.0               |
| react                   | 16.8.3                     |
| react-native            | 0.59.3                     |
| react-native-cli        | 2.0.1                      |
| react-native-navigation | 2.16.0                     |
| shadow-cljs             | 2.8.26                     |
| Xcode                   | 10.2                       |
| iOS SDK                 | 12.2                       |
| Android SDK             | API levels 26, 27, 28      |
|                         | Build tools 27.0.3, 28.0.3 |
|                         | System images: android-28  |



[wix-rnn]: https://wix.github.io/react-native-navigation
[rn]: https://facebook.github.io/react-native
[cljs]: https://clojurescript.org
[shadow-cljs]: http://shadow-cljs.org
[uinavigationcontroller]: https://developer.apple.com/documentation/uikit/uinavigationcontroller
[github]: https://github.com/svdo/CLJSReactNativeNavigation
[build-tool]: https://shadow-cljs.github.io/docs/UsersGuide.html#_build_tool_integration
[forceUpdate]: https://reactjs.org/docs/react-component.html#forceupdate
[rnn-installing]: https://wix.github.io/react-native-navigation/#/docs/Installing
[navBtnPressed]: https://wix.github.io/react-native-navigation/#/docs/events?id=navigationbuttonpressed-event
[thheller]: https://github.com/thheller
[patreon]: https://www.patreon.com/thheller
[wrapper]: https://github.com/svdo/CLJSReactNativeNavigation/blob/master/src/main/env.cljc
