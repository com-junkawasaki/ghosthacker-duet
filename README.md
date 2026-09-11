# GHOST HACKER: DUET

![test](https://github.com/com-junkawasaki/ghosthacker-duet/actions/workflows/test.yml/badge.svg)

Ghost Hacker ゲームポートフォリオ第8弾（スポーツ）。設計は
[ADR-2607023200](../../../90-docs/adr/2607023200-ghosthacker-game-portfolio-flow.md)
（超project `com-junkawasaki/root`、addendum 2まで参照）を参照。判定/score/combo/groove核は
[com-junkawasaki/ghosthacker-groove-core](https://github.com/com-junkawasaki/ghosthacker-groove-core)
（[ADR-2607032600](../../../90-docs/adr/2607032600-ghosthacker-groove-core-extraction.md)）
を共有し、このリポジトリは**player-vs-rivalの1対1対抗戦**というDUET固有の
勝敗フレーミングとホストアダプタだけを持つ。groove-coreの判定/score/combo
ロジックは複製しない（ADR-2607032600の「share, don't duplicate」方針）。

## コンセプト

- **ジャンル**: スポーツ
- **主人公**: Ren単独
- **コアループ**: 他校/他事務所との1対1対抗戦。**競技化されたGhost Battle**。
  playerと（あらかじめ走行結果が分かっている）rivalが同じ譜面(chart)を
  走り、両者の最終`:score`を比較して`:win`/`:lose`/`:draw`を決める。
  FLOW/HARMONYと同じ「四つ打ちに同期し続ける」判定モデルそのものは
  変えず、勝敗の軸を「単独の閾値」（HARMONYのASYMMETRY/HARMONY）ではなく
  「対戦相手との比較」に置き換えたのがDUETの独自性。

## 実装範囲

`src/ghosthacker_duet/match.kotoba` — pure、host-free。
`ghosthacker.groove.core`は複製せず、以下だけを追加する:

- **match-outcome** — playerとrivalの`groove/summary`同士を`:score`で
  比較し、`:win`/`:lose`/`:draw`を返す。`:groove`ではなく`:score`を
  比較軸に選んだ理由: `:groove`は0.0〜1.0のcrossfadeパラメータなので
  両者とも安定してプレイすると1.0付近で張り付き引き分けが頻発する——
  実際のスコアボードのように差が積み上がる`:score`の方が「対抗戦」の
  決着軸として自然、という設計判断（docstringに明記）。
- **match-summary** — player/rivalの`summary`に`:outcome`を足した、
  ホストアダプタのリザルト画面にそのまま渡せる形
- **play-match** — playerの入力列を`groove/chart-play-run`で評価し、
  渡された`rival-summary`と比較する統合API

`src/ghosthacker_duet/rivals.kotoba` — サンプルデータの**難易度プロファイル**
（`:steady-rival`＝堅実型、good窓中心／`:ace-rival`＝エース、perfect窓中心）。
このポートフォリオ共通ルール（新規オリジナルキャラクターは追加しない）に
従い、名前や人格を持つキャラクターとしては実装せず、あくまで「他校/他事務所の
既知の走行傾向」という決定的（deterministic）な入力オフセットパターンとして
扱う。ライブの第2プレイヤーではなく、`rival-summary`が同じ`chart`に対して
`groove/chart-play-run`をそのまま呼ぶことで走行結果を作る——rival専用の
判定ロジックはゼロで、playerの判定と全く同じgroove-core APIを使う。

**プレイ可能な最小プロトタイプ**として `src/ghosthacker_duet/terminal.kotoba`
がある（ghosthacker-harmonyのterminal.cljと同じ構成: 新規依存ゼロ、
背景`future`が実時刻でtickを刻み、`read-line`で実際の経過時間を判定）。
プレイ終了後、指定したrival（既定は`:steady-rival`）の走行結果と比較して
WIN/LOSE/DRAWを表示する。

**ブラウザで遊べるホストアダプタ**が `src/ghosthacker_duet/web.kotoba`
（reagent、ADR-2607100900 follow-up (b)、ghosthacker-flow/harmonyと同じ設計）:
作曲済みの2レイヤー楽曲は存在しないため、Web Audioの
`AudioContext.currentTime`でビートクロック+合成メトロノーム音
（オシレーター）を駆動しつつ、`:groove`は見た目のTENSE⇄Sky High
crossfadeを駆動する。スタート画面で対戦相手（難易度プロファイル）を
選べ、リザルト画面でplayer/rivalのスコアとWIN/LOSE/DRAWを並べて表示する。
Web Audio非対応環境では`performance.now()`+無音に自動degrade。

本格的なレンダリング（`kami-engine-sdk`のようなキャンバス/wasm描画）は
依然として別レイヤーの課題——現状はDOM/CSSのみ。

## 開発

```bash
kbb -M:test
```

Lint（clj-kondo、Clojars経由でHomebrew等の別インストール不要）:

```bash
kbb -M:lint
```

ターミナルで遊んでみる（第2引数でrivalを指定、既定は`steady-rival`）:

```bash
kbb -M -m ghosthacker-duet.terminal 16 ace-rival
```

ブラウザで遊んでみる（`npm install`は初回のみ、Spaceキーで入力）:

```bash
npm install
amu compile --target wasm32-browser app   # http://localhost:8301 で自動リロード開発
amu compile --target wasm32-browser app # public/ に静的バンドルをビルド(デプロイ可能)
```

変更履歴は [CHANGELOG.md](CHANGELOG.md)。
