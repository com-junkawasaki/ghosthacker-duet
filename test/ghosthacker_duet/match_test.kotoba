(ns ghosthacker-duet.match-test
  (:require [clojure.test :refer [deftest is testing]]
            [ghosthacker.groove.core :as groove]
            [ghosthacker-duet.rivals :as rivals]
            [ghosthacker-duet.match :as match]))

(defn- summary-with-score [score]
  (assoc (groove/summary groove/initial-state) :score score))

(deftest match-outcome-test
  (testing "playerのscoreがrivalより高ければ:win"
    (is (= :win (match/match-outcome (summary-with-score 1000) (summary-with-score 500)))))
  (testing "playerのscoreがrivalより低ければ:lose"
    (is (= :lose (match/match-outcome (summary-with-score 500) (summary-with-score 1000)))))
  (testing "同じscoreなら:draw"
    (is (= :draw (match/match-outcome (summary-with-score 700) (summary-with-score 700)))))
  (testing "両者スコア0(未プレイ同士)も:draw"
    (is (= :draw (match/match-outcome (summary-with-score 0) (summary-with-score 0))))))

(deftest match-summary-test
  (testing "match-summaryは:player/:rival/:outcomeを持つ"
    (let [ps (summary-with-score 1200)
          rs (summary-with-score 800)
          result (match/match-summary ps rs)]
      (is (= ps (:player result)))
      (is (= rs (:rival result)))
      (is (= :win (:outcome result))))))

(deftest play-match-test
  (testing "play-matchはplayerの入力列をchart-play-runで評価し、
            渡されたrival-summaryと比較したmatch-summaryを返す"
    (let [chart (groove/chart-beats 0 [{:bpm 124 :beat-count 8}])
          ;; playerは全拍ジャストで叩く -> 全perfect
          result (match/play-match chart chart (summary-with-score 0))]
      (is (= :win (:outcome result)))
      (is (pos? (get-in result [:player :score])))
      (is (== 1.0 (get-in result [:player :accuracy]))))))

;; --- rivals.cljc との結合（同じchartをplayerとrivalの両方が走る、という
;;     DUETの前提が実際に成り立つことを確認する） ---------------------------

(deftest rival-summary-deterministic-test
  (testing "同じchart+rival-keyなら常に同じsummaryを返す(決定的)"
    (let [chart (groove/chart-beats 0 [{:bpm 124 :beat-count 12}])
          a (rivals/rival-summary chart :steady-rival)
          b (rivals/rival-summary chart :steady-rival)]
      (is (= a b))))
  (testing "ace-rivalはsteady-rivalよりgroove/accuracyが高い
            (offset-pattern-msがperfect窓に収まるため)"
    (let [chart (groove/chart-beats 0 [{:bpm 124 :beat-count 12}])
          steady (rivals/rival-summary chart :steady-rival)
          ace (rivals/rival-summary chart :ace-rival)]
      (is (every? #(= :good %) (:judgments (groove/chart-play-run
                                              chart
                                              (rivals/rival-input-times chart
                                                (get-in rivals/profiles [:steady-rival :offset-pattern-ms]))))))
      (is (> (:groove ace) (:groove steady)))
      (is (> (:score ace) (:score steady))))))

(deftest duet-full-match-test
  (testing "playerが全拍ジャストで戦えば、ace-rival(ほぼperfect)相手でも
            スコアで上回りWIN(同点比較の設計上、全perfect同士は:score同値
            になり得るのでdrawも許容する)"
    (let [chart (groove/chart-beats 0 [{:bpm 124 :beat-count 12}])
          riv-summary (rivals/rival-summary chart :ace-rival)
          result (match/play-match chart chart riv-summary)]
      (is (contains? #{:win :draw} (:outcome result)))))
  (testing "playerが何も入力しない(空振りのみ)場合、
            steady-rival相手でも必ずLOSEになる"
    (let [chart (groove/chart-beats 0 [{:bpm 124 :beat-count 8}])
          riv-summary (rivals/rival-summary chart :steady-rival)
          player-summary (groove/chart-play-run chart [])
          result (match/match-summary player-summary riv-summary)]
      (is (= :lose (:outcome result)))
      (is (zero? (get-in result [:player :score]))))))
