(ns ghosthacker-duet.match
  "GHOST HACKER: DUET — 1v1 match core (ADR-2607023200, portfolio #8,
  スポーツ/Ren単独). 「他校/他事務所との1対1対抗戦。競技化された
  Ghost Battle」というDUET固有のフレーミングは、HARMONYのような単独プレイヤーの
  勝敗判定（groove閾値との比較）ではなく、**player-vs-rivalの比較**そのもの——
  同じchart(譜面)をplayerとrivalの両方が走り、走り終えた後の
  ghosthacker.groove.core/summary同士を突き合わせて :win/:lose/:draw を決める。

  この名前空間はその比較ロジックだけを持つ、pure・host-free。
  rivalのプロファイル(サンプルデータ + rivalの走行結果を作る関数)は
  ghosthacker-duet.rivals にある——match.cljcはrivalの中身を一切知らず、
  『2つのgroove/summaryを比較する』ことにしか関知しない(playerの走行が
  groove-coreそのものなのと同じく、rivalの走行もgroove-coreそのもので
  作られたsummaryである、という前提だけを共有する)。

  勝敗の軸には :score(累積点)を使う——:groove は0.0〜1.0にclampされる
  crossfadeパラメータなので、両者とも安定して乗り続けると1.0付近で
  張り付きやすく引き分けが頻発する。競技化されたGhost Battleという
  スポーツのフレーミングには、実際のスコアボードのように差が積み上がる
  :score の方が「対抗戦」の勝敗軸として自然——ADR-2607023200 のDUET設計
  （「他校/他事務所との1対1対抗戦」）が求めるのは僅差でも決着がつく
  比較であって、grooveの飽和で引き分けだらけになる比較ではない。"
  (:require [ghosthacker.groove.core :as groove]))

(defn match-outcome
  "player-summaryとrival-summary（どちらもghosthacker.groove.core/summaryの
   戻り値の形、:scoreキーを持つマップ）を:scoreで比較し、
   :win(player勝ち)/:lose(player負け)/:draw(同点)を返す。"
  [player-summary rival-summary]
  (let [player-score (:score player-summary)
        rival-score (:score rival-summary)]
    (cond
      (> player-score rival-score) :win
      (< player-score rival-score) :lose
      :else :draw)))

(defn match-summary
  "player-summary/rival-summaryに:outcome(match-outcomeの結果)を足した、
   ホストアダプタのリザルト画面にそのまま渡せる形のマップを返す。
   {:player player-summary :rival rival-summary :outcome outcome}"
  [player-summary rival-summary]
  {:player player-summary
   :rival rival-summary
   :outcome (match-outcome player-summary rival-summary)})

(defn play-match
  "player-input-times(playerの入力列)をchartに対してgroove/chart-play-runで
   評価し、既に計算済みのrival-summary(ghosthacker-duet.rivals/rival-summary
   の戻り値)と突き合わせてmatch-summaryを返す。playerの走行そのものは
   groove-coreの標準API(chart-play-run = chart-run + summary)をそのまま
   使う——FLOW/HARMONYと同じ流儀で、DUET固有のロジックを走行判定に混ぜない。"
  [chart player-input-times rival-summary]
  (let [player-summary (groove/chart-play-run chart player-input-times)]
    (match-summary player-summary rival-summary)))
