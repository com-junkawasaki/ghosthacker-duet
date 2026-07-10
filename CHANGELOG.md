# Changelog

pure `.cljc` 1v1 match core（`ghosthacker-duet.match`）と、それを使う
プロトタイプ実装の変更履歴（ADR-2607023200 / ADR-2607032600）。

## Unreleased

- 初期実装: `match.cljc`（`match-outcome`/`match-summary`/`play-match`、
  `ghosthacker.groove.core`の`chart-play-run`/`summary`をそのまま使い、
  比較軸として`:groove`ではなく`:score`を選んだ設計判断をdocstringに明記）、
  `rivals.cljc`（サンプル難易度プロファイル`:steady-rival`/`:ace-rival`、
  新規オリジナルキャラクターは追加せず決定的なoffset-pattern-msのみ）、
  `terminal.clj`（プレイ可能なターミナルプロトタイプ、プレイ後にrivalの
  走行結果と比較しWIN/LOSE/DRAWを表示）。12 tests / 31 assertions。
- ブラウザhostアダプタ追加（ADR-2607100900 follow-up (b)、
  ghosthacker-flow/harmonyと同じ設計）: `web.cljs`（reagent、Web Audioで
  ビートクロック+合成メトロノーム音、`:groove`は視覚的TENSE⇄Sky High
  クロスフェード、スタート画面でrival選択、リザルト画面でplayer/rivalの
  スコア比較とWIN/LOSE/DRAWを表示）+ `shadow-cljs.edn`/`package.json`/
  `public/index.html`。headless DOM上で実keydown/click操作による通し
  （START→カウントダウン→12拍judge→WIN/LOSE/DRAW判定表示→もう一度で
  初期状態に復帰）を検証済み。
