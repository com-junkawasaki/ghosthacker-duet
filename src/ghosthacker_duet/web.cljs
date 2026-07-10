(ns ghosthacker-duet.web
  "GHOST HACKER: DUET -- browser host adapter (ADR-2607100900 follow-up
  (b)). Same shape as ghosthacker-flow/ghosthacker-harmony's web.cljs
  (ClojureScript, since real-time beat timing + audio are host-imports
  neither kotoba wasm nor clojurewasm can provide yet, ADR-2607100030
  addendum 2), driving plain `ghosthacker.groove.core` for the player's
  run -- DUET needs no HARMONY-style per-tick state wrapper. What makes
  this DUET: once the player's chart ends, the result screen also
  evaluates a fixed, deterministic rival profile
  (`ghosthacker-duet.rivals`) against the *same* chart and compares both
  final summaries via `ghosthacker-duet.match`, showing a win/lose/draw
  verdict for the 1v1 competitive framing (ADR-2607023200, portfolio #8)
  instead of FLOW's solo grade or HARMONY's ASYMMETRY/HARMONY latch.

  No composed two-layer music exists for :groove to crossfade between,
  so Web Audio drives both the beat clock (AudioContext.currentTime) and
  a synthesized metronome tick, while :groove drives a visual TENSE
  (cool)<->Sky High(warm) crossfade, same as FLOW/HARMONY. Falls back to
  performance.now() with no audible tick when Web Audio is unavailable."
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [ghosthacker.groove.core :as core]
            [ghosthacker-duet.rivals :as rivals]
            [ghosthacker-duet.match :as match]))

;; --- clock / audio ----------------------------------------------------

(defonce ^:private !ctx (atom nil))

(defn- ensure-ctx! []
  (when-not @!ctx
    (when-let [ctor (or (.-AudioContext js/window) (.-webkitAudioContext js/window))]
      (reset! !ctx (new ctor))))
  @!ctx)

(defn- now-ms []
  (if-let [ctx @!ctx]
    (* 1000 (.-currentTime ctx))
    (.now js/performance)))

(defn- schedule-tick! [t-ms freq]
  (when-let [ctx @!ctx]
    (let [t (/ t-ms 1000.0)
          osc (.createOscillator ctx)
          gain (.createGain ctx)]
      (set! (.-value (.-frequency osc)) freq)
      (.setValueAtTime (.-gain gain) 0.001 t)
      (.linearRampToValueAtTime (.-gain gain) 0.25 (+ t 0.005))
      (.exponentialRampToValueAtTime (.-gain gain) 0.001 (+ t 0.09))
      (.connect osc gain)
      (.connect gain (.-destination ctx))
      (.start osc t)
      (.stop osc (+ t 0.1)))))

;; --- chart --------------------------------------------------------------

(defn- default-chart
  "Same 2-section TENSE(default bpm) -> Sky High(1.25x) shape as
  terminal.clj's default-chart. Player and rival both race this same
  chart -- that's what makes the comparison an apples-to-apples match."
  [start-time-ms beat-count]
  (let [half (quot beat-count 2)
        rest-count (- beat-count half)]
    (core/chart-beats start-time-ms
                       [{:bpm core/default-bpm :beat-count half}
                        {:bpm (long (* 1.25 core/default-bpm)) :beat-count rest-count}])))

;; --- state ----------------------------------------------------------------

(defonce state
  (r/atom {:phase :idle          ; :idle | :countdown | :playing | :result
           :beat-count 12
           :rival-key :steady-rival
           :chart nil
           :groove-state nil
           :beats-done 0
           :last-judgment nil
           :countdown-label "3"}))

(defn- hit! []
  (when (= (:phase @state) :playing)
    (let [t (now-ms)
          {:keys [chart groove-state beats-done]} @state
          next-gs (core/judge-chart-input groove-state chart t)
          judgment (last (:judgments next-gs))
          done (inc beats-done)]
      (swap! state assoc
             :groove-state next-gs
             :last-judgment judgment
             :beats-done done
             :phase (if (>= done (count chart)) :result :playing)))))

(defn- start-game! []
  (ensure-ctx!)
  (let [beat-count (:beat-count @state)
        interval (core/beat-interval-ms core/default-bpm)
        go-time-ms (+ (now-ms) (* 3 interval))
        chart (default-chart go-time-ms beat-count)]
    (swap! state assoc
           :phase :countdown
           :chart chart
           :groove-state core/initial-state
           :beats-done 0
           :last-judgment nil
           :countdown-label "3")
    (schedule-tick! (- go-time-ms (* 3 interval)) 440)
    (schedule-tick! (- go-time-ms (* 2 interval)) 440)
    (schedule-tick! (- go-time-ms interval) 440)
    (doseq [t chart] (schedule-tick! t 880))
    (doseq [[i label] (map-indexed vector ["3" "2" "1"])]
      (js/setTimeout #(swap! state assoc :countdown-label label) (* i interval)))
    (js/setTimeout #(swap! state assoc :countdown-label "GO!" :phase :playing) (* 3 interval))))

(defn- restart! [] (swap! state assoc :phase :idle))

(defn- select-rival! [rival-key]
  (swap! state assoc :rival-key rival-key))

;; --- keyboard input ---------------------------------------------------

(defn- on-keydown [e]
  (when (= (.-code e) "Space")
    (.preventDefault e)
    (hit!)))

;; --- views ------------------------------------------------------------

(defn- groove-bg
  "TENSE(g=0, cool blue) -> Sky High(g=1, warm gold), same crossfade as
  FLOW/HARMONY."
  [g]
  (let [hue (- 220 (* g 180))]
    {:background (str "linear-gradient(135deg, hsl(" hue ",70%,14%), hsl(" hue ",70%,24%))")}))

(defn- start-screen []
  (let [rival-key (:rival-key @state)]
    [:div.duet-app
     [:h1 "GHOST HACKER: DUET"]
     [:p.duet-sub "1v1対抗戦 — 同じ譜面を対戦相手と競う。Space で入力。"]
     [:div.duet-rival-select
      (for [[k {:keys [label]}] rivals/profiles]
        ^{:key k}
        [:button.duet-rival-btn
         {:class (when (= k rival-key) "duet-rival-btn-selected")
          :on-click #(select-rival! k)}
         label])]
     [:button {:on-click start-game!} "START"]]))

(defn- countdown-screen []
  [:div.duet-app
   [:h1 "GHOST HACKER: DUET"]
   [:div.duet-countdown (:countdown-label @state)]])

(defn- playing-screen []
  (let [{:keys [groove-state last-judgment beats-done chart]} @state]
    [:div.duet-app {:style (groove-bg (:groove groove-state))}
     [:h1 "GHOST HACKER: DUET"]
     [:div.duet-hud
      [:span (str "beat " beats-done "/" (count chart))]
      [:span (str "combo " (:combo groove-state))]
      [:span (str "groove " (.toFixed (:groove groove-state) 2))]]
     [:div.duet-judgment (when last-judgment (name last-judgment))]
     [:p.duet-hint "Space で入力"]]))

(defn- result-screen []
  (let [{:keys [chart groove-state rival-key]} @state
        player-summary (core/summary groove-state)
        riv-summary (rivals/rival-summary chart rival-key)
        result (match/match-summary player-summary riv-summary)
        outcome (:outcome result)
        rival-label (get-in rivals/profiles [rival-key :label])]
    [:div.duet-app
     [:h1 "GHOST HACKER: DUET"]
     [:h2 {:class (case outcome
                    :win "duet-win"
                    :lose "duet-lose"
                    :draw "duet-draw")}
      (case outcome
        :win "WIN — 対抗戦を制した。"
        :lose "LOSE — 対抗戦に敗れた。"
        :draw "DRAW — 引き分け。")]
     [:p (str "player: score " (:score player-summary) " / groove " (.toFixed (:groove player-summary) 2))]
     [:p (str "vs " rival-label ": score " (:score riv-summary) " / groove " (.toFixed (:groove riv-summary) 2))]
     [:button {:on-click restart!} "もう一度"]]))

(defn app []
  (case (:phase @state)
    :countdown [countdown-screen]
    :playing [playing-screen]
    :result [result-screen]
    [start-screen]))

(defn ^:export mount []
  (when-let [el (.getElementById js/document "app")]
    (.addEventListener js/window "keydown" on-keydown)
    (rdom/render [app] el)))

(defn ^:export init [] (mount))
