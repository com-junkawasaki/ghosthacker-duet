(ns ghosthacker-duet.terminal
  "GHOST HACKER: DUET — minimal terminal host adapter (playable prototype).

  Same shape as com-junkawasaki/ghosthacker-flow's terminal.clj (a background
  `future` ticks to each beat's wall-clock time while the main thread judges
  `read-line` timing against the real elapsed time) -- the player's run is
  plain `ghosthacker.groove.core`, no HARMONY-style state wrapper needed.
  What makes this DUET instead of FLOW: after the player's run ends, this
  namespace also evaluates a fixed, deterministic rival profile
  (`ghosthacker-duet.rivals`) against the *same* chart, then compares both
  final summaries with `ghosthacker-duet.match/match-outcome` to print a
  win/lose/draw verdict -- the 1v1 competitive framing (ADR-2607023200,
  portfolio #8) instead of FLOW's solo grade or HARMONY's ASYMMETRY/HARMONY
  latch.

  Run: clojure -M -m ghosthacker-duet.terminal [beat-count] [rival-key]
  rival-key is :steady-rival (default) or :ace-rival."
  (:require [ghosthacker.groove.core :as groove]
            [ghosthacker-duet.rivals :as rivals]
            [ghosthacker-duet.match :as match]))

(defn- print-tick! []
  (print "♪ ")
  (flush))

(defn- run-ticker!
  "chart(絶対ms時刻の列)どおりにtickを印字するfutureを起動する。
   呼び出し側はゲーム終了時にfuture-cancelで止めること。"
  [chart]
  (future
    (doseq [t chart]
      (let [wait (- t (System/currentTimeMillis))]
        (when (pos? wait)
          (Thread/sleep wait)))
      (print-tick!))))

(defn- countdown!
  "「3, 2, 1, GO!」を1拍分の間隔で表示する。"
  [bpm]
  (let [interval-ms (long (groove/beat-interval-ms bpm))]
    (doseq [n [3 2 1]]
      (println n)
      (Thread/sleep interval-ms))
    (println "GO!")))

(defn- read-beats!
  "chart(拍の絶対時刻ms列)ぶんread-lineで入力を待ち、
   groove/judge-chart-inputで都度判定して進行状況を印字する。
   標準入力がEOF(nil)になったら、そこまでのstateで打ち切る。"
  [chart]
  (loop [state groove/initial-state i 0]
    (if (>= i (count chart))
      state
      (let [line (read-line)]
        (if (nil? line)
          state
          (let [now (System/currentTimeMillis)
                next-state (groove/judge-chart-input state chart now)
                judgment (last (:judgments next-state))]
            (println (format " -> %s (combo %d)" (name judgment) (:combo next-state)))
            (recur next-state (inc i))))))))

(defn- default-chart
  "既定の曲構成: 前半(既定bpm)→後半(1.25倍速)へ加速する2セクション。
   FLOW/HARMONYと同じ形——playerとrivalは同じchartを走るので、
   この構成自体がDUETの『対戦条件を揃える』土台になる。"
  [start-time-ms beat-count]
  (let [half (quot beat-count 2)
        rest-count (- beat-count half)]
    (groove/chart-beats start-time-ms
                        [{:bpm groove/default-bpm :beat-count half}
                         {:bpm (long (* 1.25 groove/default-bpm)) :beat-count rest-count}])))

(defn- parse-rival-key [s]
  (if s (keyword s) :steady-rival))

(defn -main
  "Entry point for `clojure -M -m ghosthacker-duet.terminal [beat-count] [rival-key]`.
  See the ns docstring."
  [& args]
  (let [beat-count (if-let [a (first args)] (Integer/parseInt a) 12)
        rival-key (parse-rival-key (second args))
        rival (get rivals/profiles rival-key)]
    (println (format "GHOST HACKER: DUET — 1v1対抗戦 (%d beats, vs %s)"
                      beat-count (:label rival)))
    (println "Enterキーで各拍を叩いてください。準備ができたらEnterで開始:")
    (read-line)
    (countdown! groove/default-bpm)
    (let [start-time-ms (System/currentTimeMillis)
          chart (default-chart start-time-ms beat-count)
          ticker (run-ticker! chart)
          player-state (read-beats! chart)]
      (future-cancel ticker)
      (println)
      (println "=== DUET RESULT ===")
      (let [player-summary (groove/summary player-state)
            riv-summary (rivals/rival-summary chart rival-key)
            result (match/match-summary player-summary riv-summary)]
        (println (format "player: score=%d max-combo=%d accuracy=%.2f groove=%.2f"
                          (:score player-summary)
                          (:max-combo player-summary)
                          (double (:accuracy player-summary))
                          (double (:groove player-summary))))
        (println (format "rival (%s): score=%d accuracy=%.2f groove=%.2f"
                          (:label rival)
                          (:score riv-summary)
                          (double (:accuracy riv-summary))
                          (double (:groove riv-summary))))
        (println (case (:outcome result)
                   :win "WIN — 対抗戦を制した。"
                   :lose "LOSE — 対抗戦に敗れた。"
                   :draw "DRAW — 引き分け。")))
      ;; futureはclojure.lang.Agentの非daemonスレッドプールを使うため、
      ;; これを呼ばないとロジック完了後もJVMプロセスが終了せずハングする。
      (shutdown-agents))))
