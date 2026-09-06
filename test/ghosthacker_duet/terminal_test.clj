(ns ghosthacker-duet.terminal-test
  "terminalの`-main`はshutdown-agentsを呼ぶため、共有JVMで動くテスト
   プロセス全体のagentスレッドプールを止めてしまう(以降のテストが
   futureを使えなくなる)。よって-mainそのものはテストせず、read-beats!
   （private var経由）を直接叩く。実プロセスとしての-main自体は
   手動検証済み(EOF/連打とも正しく完了しプロセスがハングしないことを確認)。"
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ghosthacker.groove.core :as groove]
            [ghosthacker-duet.terminal :as terminal]))

(def ^:private read-beats! #'terminal/read-beats!)
(def ^:private countdown! #'terminal/countdown!)
(def ^:private default-chart #'terminal/default-chart)
(def ^:private parse-rival-key #'terminal/parse-rival-key)

(defn- silently [thunk]
  ;; swallow stdout while still returning thunk's value; with-out-str is
  ;; portable (JVM + ClojureScript), so no java.io.StringWriter import.
  (let [ret (promise)]
    (with-out-str (deliver ret (thunk)))
    @ret))

(defn- capture-out [thunk]
  (with-out-str (thunk)))

(deftest read-beats-eof-boundary-test
  (testing "stdinがEOF(空)ならjudgmentゼロのまま即座に打ち切る(ハングしない)"
    (let [chart (groove/beat-schedule 120 (System/currentTimeMillis) 4)
          state (silently #(with-in-str "" (read-beats! chart)))]
      (is (= [] (:judgments state)))
      (is (zero? (:score state))))))

(deftest read-beats-anti-mash-guard-test
  (testing "複数行を即座に読んでも例外にならない。最初の入力はperfect/goodのいずれか
            (JVM起動オーバーヘッドの誤差を許容)、以降は同じ拍への対マッシュガードで
            全てmissになる"
    (let [chart (groove/beat-schedule 120 (System/currentTimeMillis) 4)
          state (silently #(with-in-str "\n\n\n\n" (read-beats! chart)))]
      (is (= 4 (count (:judgments state))))
      (is (not= :miss (first (:judgments state))))
      (is (every? #(= :miss %) (rest (:judgments state)))))))

(deftest read-beats-partial-input-boundary-test
  (testing "beat-count分に満たない入力(途中でEOF)は、そこまでのjudgmentsで打ち切る"
    (let [chart (groove/beat-schedule 120 (System/currentTimeMillis) 5)
          state (silently #(with-in-str "\n\n" (read-beats! chart)))]
      (is (= 2 (count (:judgments state)))))))

(deftest read-beats-returns-plain-groove-state-test
  (testing "read-beats!の戻りはgroove-coreのplain state(:score/:groove等を持つ)"
    (let [chart (groove/beat-schedule 120 (System/currentTimeMillis) 4)
          state (silently #(with-in-str "" (read-beats! chart)))]
      (is (contains? state :score))
      (is (contains? state :groove))
      (is (== 0.0 (:groove state))))))

(deftest default-chart-test
  (testing "beat-count分の拍を返し、後半セクションは前半より拍間隔が短い(加速)"
    (let [chart (default-chart 0 12)]
      (is (= 12 (count chart)))
      (is (< (- (nth chart 7) (nth chart 6)) (- (nth chart 1) (nth chart 0)))))))

(deftest countdown-test
  (testing "3, 2, 1, GO!の順で表示される（高bpmでsleepを短くしてテストを速くする）"
    (let [output (capture-out #(countdown! 6000))]
      (is (= ["3" "2" "1" "GO!"] (str/split-lines output))))))

(deftest parse-rival-key-test
  (testing "引数無しなら既定の:steady-rival"
    (is (= :steady-rival (parse-rival-key nil))))
  (testing "文字列はkeywordへ変換される"
    (is (= :ace-rival (parse-rival-key "ace-rival")))))
