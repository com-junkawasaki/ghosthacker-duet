(ns ghosthacker-duet.rivals
  "GHOST HACKER: DUET — rival profiles (sample data, ADR-2607023200).

  DUETの対戦相手は『他校/他事務所』というジャンル設定上の枠でしかなく、
  この移植ポートフォリオ共通ルール（新規オリジナルキャラクターは追加しない）
  に従い、名前も人格も持つキャラクターとしては実装しない——ここにあるのは
  『同じchartに対する、既知の(=あらかじめ分かっている)対戦相手の走行傾向』
  という**難易度プロファイル**だけ。ライブの第2プレイヤーが実際に入力する
  わけではなく、決定的（deterministic）な入力オフセット列をあらかじめ
  スクリプトとして持たせ、それをchartのビート列にそのまま重ねて
  groove-coreで判定させることで『rivalの走行結果』を作る——rival専用の
  判定ロジックは一切書かず、player側と全く同じgroove-coreのAPI
  (chart-play-run)を使う。

  :offset-pattern-ms は各拍への入力タイミングのずれ(ms、符号付き)を
  循環的に割り当てる固定パターン——ランダムではなく完全に決定的なので、
  同じchartに対するrivalの走行結果は常に再現可能(テスト可能)。"
  (:require [ghosthacker.groove.core :as groove]))

(def profiles
  "難易度プロファイル。offset-pattern-msの各値の絶対値が
   groove-coreのperfect-window-ms(30)/good-window-ms(80)のどちらの
   窓に収まるかで、rivalの走行が:perfect寄りか:good寄りかが決まる。"
  {;; 手堅く:good判定を積み重ねる格上ではない相手 -- perfect窓(30ms)は
   ;; 外すが、good窓(80ms)には毎回収まる。
   :steady-rival
   {:label "他校の対戦相手（堅実型）"
    :offset-pattern-ms [45 -50 42 -48 44 -46]}

   ;; ほぼ毎回perfect窓(30ms)に収める強豪。
   :ace-rival
   {:label "他事務所のエース"
    :offset-pattern-ms [5 -8 10 -6 7 -4]}})

(defn rival-input-times
  "chart(拍の絶対時刻ms列)の各拍にoffset-pattern-ms(循環)を足した、
   rivalの決定的な入力時刻列を返す。"
  [chart offset-pattern-ms]
  (let [n (count offset-pattern-ms)]
    (mapv (fn [i t] (+ t (nth offset-pattern-ms (mod i n))))
          (range (count chart))
          chart)))

(defn rival-summary
  "profiles中のrival-key(:steady-rival/:ace-rival)を、同じchartに対して
   走らせた結果のghosthacker.groove.core/summaryを返す——playerの走行と
   全く同じgroove/chart-play-runを使うので、player-summaryと
   ghosthacker-duet.match/match-outcomeで直接比較できる形になる。"
  [chart rival-key]
  (let [{:keys [offset-pattern-ms]} (get profiles rival-key)
        input-times (rival-input-times chart offset-pattern-ms)]
    (groove/chart-play-run chart input-times)))
