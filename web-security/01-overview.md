# 第1回：ライブラリ汚染の全体像 — 攻撃面マップを共有する

## この回の目的

ライブラリ汚染を「怪しいパッケージに注意しよう」という個人の心がけではなく、OSSサプライチェーン攻撃として捉え、開発プロセス全体のどこが攻撃対象になるかを全員で共有する。

第2回以降の技術的な話に入る前に、フロントエンド担当もバックエンド担当も、ジュニアもシニアも、同じ地図を持つことがこの回のゴールである。

---

## 1. 現代のWebアプリケーション開発と依存関係

> **この章の目的**：自分たちのコードよりも外部ライブラリのコードのほうが圧倒的に多いという現実と、依存関係を追加するたびに信頼境界が広がっていくという感覚を共有する。

### 1.1 プロダクトコードの実態

現代のWebアプリケーションでは、自分たちが書いたコードよりも外部ライブラリのコードのほうが圧倒的に多い。

```
典型的なReactアプリケーション:
  package.json に記載した direct dependency    : 30〜80個
  node_modules に入る transitive dependency    : 500〜1,500個
  node_modules のファイル数                     : 数万〜十数万

典型的なKotlin/Spring Bootアプリケーション:
  build.gradle.kts に記載した direct dependency : 20〜50個
  依存解決後の transitive dependency            : 100〜400個
  Gradle plugin                                 : 5〜15個
```

ここで重要なのは、これらすべてが「アプリケーションの一部」として動作するということである。直接追加したライブラリだけでなく、その依存先の依存先も、ビルド環境や実行環境に入り込む。

### 1.2 依存関係は信頼境界を広げる操作

ライブラリを追加するという行為は、機能を追加する行為であると同時に、**信頼境界を広げる行為**である。

```
信頼境界の広がり方:

  自分たちのコード
    └─ direct dependency A
         └─ transitive dependency A-1
         └─ transitive dependency A-2
              └─ transitive dependency A-2-1  ← ここまで信頼している
    └─ direct dependency B (Gradle plugin)
         └─ ビルド時にコード実行             ← ここも信頼している
    └─ direct dependency C (GitHub Action)
         └─ CI/CD上でコード実行              ← ここも信頼している
```

依存関係を追加するたびに、この信頼の範囲が広がる。そして、攻撃者はこの信頼の連鎖のどこか一箇所を侵害すればよい。

> **この章のまとめ**：依存関係の追加は機能追加であると同時に信頼境界の拡大である。攻撃者にとっては、この広がった信頼の連鎖のどこか一箇所を突けばよい、という構造を覚えておく。

---

## 2. 攻撃対象は本番アプリだけではない

> **この章の目的**：従来のWebセキュリティ教育が想定してきた「本番アプリケーションの実行時」という攻撃対象のイメージを、開発・ビルド・リリースのプロセス全体へと広げる。

### 2.1 従来のWebセキュリティ教育との違い

従来のWebセキュリティ教育が扱ってきた攻撃対象：

| 攻撃 | 対象 | タイミング |
|------|------|-----------|
| XSS | ブラウザ上のユーザー | アプリケーション実行時 |
| SQL Injection | データベース | アプリケーション実行時 |
| CSRF | ユーザーのセッション | アプリケーション実行時 |
| 認証認可不備 | APIエンドポイント | アプリケーション実行時 |

ライブラリ汚染・サプライチェーン攻撃が狙う対象：

| 攻撃対象 | 狙われるもの | タイミング |
|----------|-------------|-----------|
| 開発者端末 | 環境変数、認証情報、SSH鍵 | npm install / gradle build 時 |
| 依存解決経路 | npmレジストリ、Maven Central | 依存解決時 |
| package registry | パッケージの公開経路 | パッケージ公開時 |
| GitHub repository | コード、workflow定義 | push / PR merge時 |
| dependency cache | キャッシュされた成果物 | ビルド時 |
| CI/CD secrets | deploy token、cloud credential | CI/CDジョブ実行時 |
| build artifact | 成果物の改ざん | ビルド・リリース時 |

**攻撃対象が「本番で動くアプリケーション」から「開発・ビルド・リリースのプロセス全体」に広がっている。**

### 2.2 攻撃者が狙う認証情報

ライブラリ汚染で特に狙われるのは認証情報である。

```
狙われる認証情報の例:

  開発者端末:
    - ~/.npmrc に保存された npm token
    - ~/.gradle/gradle.properties の認証情報
    - ~/.aws/credentials
    - 環境変数に設定された API key
    - GitHub PAT (Personal Access Token)
    - SSH秘密鍵

  CI/CD環境:
    - GitHub Actions secrets
    - deploy用のcloud credential
    - npm publish token
    - Docker registry credential
    - signing key
```

悪性パッケージがこれらを収集して外部に送信するパターンは、現実に繰り返し発生している。

> **この章のまとめ**：ライブラリ汚染が狙うのは「本番で動くアプリケーション」だけではなく、開発者端末・CI/CD・registryに散らばる認証情報も含めた開発プロセス全体である。

---

## 3. ライブラリ汚染の攻撃パターン

> **この章の目的**：ライブラリ汚染が具体的にどのような手口で行われるのかを、typosquattingやdependency confusionなどの主要パターンと実際の事例を通じて把握する。

### 3.1 主要な攻撃手法

#### typosquatting（タイポスクワッティング）

正規パッケージ名に似た偽パッケージを公開し、タイポによるインストールを狙う。

```
正規:  lodash
偽物:  1odash, lodahs, lodashs

正規:  colors
偽物:  coIors (大文字のI)

正規:  @angular/core
偽物:  angular-core (scopeなし)
```

#### dependency confusion（依存関係の混乱）

社内のprivate registryで使っているパッケージ名と同じ名前のパッケージを、public registryに高いバージョン番号で公開する。パッケージマネージャーがpublic registryを優先する設定になっていると、攻撃者のパッケージがインストールされる。

```
社内 private registry:  @company/utils  v1.2.3
public npm registry:     company-utils   v99.0.0  ← 攻撃者が公開

パッケージマネージャーの設定によっては、
v99.0.0 が「より新しい」として選択される
```

#### maintainerアカウントの乗っ取り

> 用語：**maintainer**とは、そのパッケージの新バージョン公開やリポジトリ設定の変更を行える管理権限を持つ人のことである。OSSパッケージの多くは、個人またはごく少数のmaintainerによって管理されている。

正規パッケージのmaintainerアカウントを侵害し、正規の公開経路から悪性バージョンを公開する。

```
攻撃の流れ:
  1. maintainerのnpmアカウントに侵入（credential流出、フィッシング等）
  2. 正規パッケージの新バージョンを公開
  3. 新バージョンに悪性コードを含める
  4. 既存ユーザーが通常の依存更新で取得
```

この攻撃が厄介なのは、パッケージ名もregistry名も正規であるため、通常の注意では気づきにくい点にある。

#### publish tokenの流出

> 用語：**publish token**とは、npmなどのpackage registryに新バージョンを公開するための認証情報である。このtokenを持っている人は、本人がログイン操作をしなくてもそのパッケージの新バージョンを公開できる。

maintainerのpublish tokenがGitHub repositoryやCI/CDログに露出し、攻撃者がそのtokenでパッケージを公開する。

```
攻撃の流れ:
  1. maintainerがCI/CDの環境変数にpublish tokenを設定している
  2. ログ出力や誤ってpublic repositoryにコミットしたファイルからtokenが露出する
  3. 攻撃者がそのtokenを使い、悪性バージョンを正規パッケージ名で公開する
  4. パッケージ名・registry名は正規のままなので、利用者は気づきにくい
```

#### CI/CD workflowの悪用

GitHub ActionsなどのCI/CD workflowを改ざんし、ビルドやリリースの過程で悪性コードを注入する。

```
例:
  1. 利用しているthird-party GitHub Actionが侵害される
  2. workflowがそのActionをバージョンtag（例: v1）で参照している
  3. 同じtagのまま、参照先に悪性コードを含むcommitが上書きされる
  4. 次回のCI実行時に、利用者は何も変更していないのに悪性コードが動く
  5. CI上のsecrets（deploy tokenなど）が外部に送信される
```

#### registryや配布物の差し替え

registryインフラ自体を侵害するか、registryとクライアントの間で配布物を差し替える。

```
例:
  1. registryサーバーや配布用のCDN・ミラーが侵害される
  2. 利用者がnpm install / gradle buildを実行すると、
     正規パッケージのつもりで悪性に差し替えられたファイルがダウンロードされる
  3. ソースコードやpackage.json上は正規のままなので、
     ハッシュ検証やlockfileの整合性チェックがなければ気づけない
```

### 3.2 事例：Shai-Hulud（2025年）

CISAは2025年9月、npmエコシステムへの大規模サプライチェーン侵害について警告を発した。

- Shai-Huludと呼ばれるこの侵害では、500以上のnpmパッケージが影響を受けた
- 攻撃の目的は、GitHub PAT、AWS・GCP・AzureなどのクラウドAPIキーの窃取
- 正規のパッケージ公開経路が侵害され、正規に見えるパッケージから認証情報が流出

この事例は「有名なライブラリだから安全」「正規のregistryから取得しているから安全」という前提が成り立たないことを示している。

### 3.3 その他の主要な事例

| 年 | 事例 | 概要 |
|----|------|------|
| 2018 | event-stream | 人気npmパッケージのmaintainer権限が移譲され、悪性コードが注入された。特定の暗号通貨ウォレットを標的にした |
| 2021 | ua-parser-js | 週間DL数数千万のnpmパッケージのmaintainerアカウントが侵害され、暗号通貨マイナーとパスワード窃取ツールが注入された |
| 2021 | Codecov | CI/CDツールのBash Uploaderスクリプトが改ざんされ、CI環境の環境変数（credential含む）が外部に送信された |
| 2021 | dependency confusion研究 | Alex Birsan氏がApple、Microsoft、PayPalなどの社内パッケージ名をpublic registryに公開し、dependency confusionの実現可能性を実証した |
| 2024 | XZ Utils | Linux圧縮ライブラリのmaintainerとして長期間活動した攻撃者が、バックドアを仕込んだ。sshd認証バイパスが目的とされる |
| 2025 | tj-actions/changed-files | 人気GitHub Actionが侵害され、CI/CDログにsecretsが露出するよう改ざんされた |

> **この章のまとめ**：typosquattingのようにわかりやすい手口だけでなく、maintainerアカウントの乗っ取りやCI/CD workflowの改ざんのように「正規の経路」を装う手口が多数存在し、現実の事例として繰り返し発生している。

---

## 4. 攻撃面マップ — 自分たちの開発フローを整理する

> **この章の目的**：これまでに見た攻撃対象・攻撃パターンを、自分たちの開発フロー（開発者端末 → GitHub → registry → CI/CD → リリース）に当てはめた攻撃面マップとして整理する。

### 4.1 攻撃面マップの構成要素

以下の要素が、ライブラリ汚染の攻撃対象になり得る。

```
┌─────────────────────────────────────────────────────────────┐
│                     攻撃面マップ                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [開発者端末]                                                │
│    - IDE / エディタ                                          │
│    - npm / Gradle / Maven                                   │
│    - 環境変数、認証情報                                       │
│    - ローカルの dependency cache                              │
│         │                                                   │
│         ▼                                                   │
│  [GitHub Repository]                                        │
│    - ソースコード                                             │
│    - package.json / build.gradle.kts / pom.xml              │
│    - lockfile (package-lock.json, gradle.lockfile)           │
│    - workflow定義 (.github/workflows/)                       │
│    - branch protection / merge rule                         │
│         │                                                   │
│         ▼                                                   │
│  [Package Registry]                                         │
│    - npm registry (public / private)                        │
│    - Maven Central / Gradle Plugin Portal                   │
│    - 社内 Artifact Repository (Nexus, Artifactory等)         │
│         │                                                   │
│         ▼                                                   │
│  [CI/CD]                                                    │
│    - GitHub Actions workflow                                │
│    - build job / test job / deploy job                      │
│    - secrets (token, credential)                            │
│    - dependency cache / build cache                         │
│    - artifact (build output)                                │
│         │                                                   │
│         ▼                                                   │
│  [リリース経路]                                               │
│    - npm publish / Maven deploy                             │
│    - container image push                                   │
│    - cloud deployment                                       │
│    - CDN配信                                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 攻撃面マップと実業務の対応

攻撃面マップの各要素は、抽象的な箱ではなく、普段の開発・リリース・運用・保守の業務の中で実際に触れているものである。

| 攻撃面マップの要素 | 主に関係する業務 | 具体的な場面の例 |
|---|---|---|
| 開発者端末 | 開発 | 新しいライブラリを `npm install` / `./gradlew build` で追加・更新する時、ローカルでビルド・テストを実行する時 |
| GitHub Repository | 開発（レビュー・マージ） | PRを作成・レビュー・mergeする時、Dependabot/Renovateの依存更新PRをmergeする時、workflow定義を変更する時 |
| Package Registry | 開発・リリース | `npm install` / `gradle build` 時の依存解決、自社パッケージやライブラリをpublishする時 |
| CI/CD | 開発・リリース（自動化されたビルド・テスト・デプロイ） | PRごとのCI実行、リリースブランチのビルド・デプロイジョブ実行、CIキャッシュの利用 |
| リリース経路 | リリース・運用 | デプロイ作業、コンテナイメージのpush、CDNへの配信更新 |

つまり、「いつ攻撃面に触れるか」という観点では、

- **開発フェーズ**：ライブラリの追加・更新、PRレビュー・マージのたびに、開発者端末・GitHub Repository・Package Registryに触れている
- **リリースフェーズ**：デプロイやpublish作業のたびに、CI/CDとリリース経路に触れている
- **運用・保守フェーズ**：Dependabot/Renovateによる定期的な依存更新の確認・マージ、CI/CDのsecrets管理やworkflowの見直しも、攻撃面に触れる業務である

「攻撃面マップ＝普段やっている作業そのもの」であり、特別な作業の時だけ注意すればよいわけではない、という点を意識する。

### 4.3 攻撃の流れの例

#### パターンA：悪性パッケージ → 開発者端末 → 認証情報流出

```
1. 攻撃者が typosquatting パッケージを公開
2. 開発者が誤って npm install
3. install script が実行される
4. 環境変数から npm token / GitHub PAT / AWS credential を収集
5. 攻撃者のサーバーに送信
6. 攻撃者は入手した credential で追加の攻撃が可能
```

#### パターンB：悪性パッケージ → CI/CD → deploy credential流出

```
1. 正規パッケージの maintainer アカウントが侵害される
2. 悪性バージョンが公開される
3. Dependabot/Renovate が自動で更新PRを作成
4. レビューで見落とされ merge される
5. CI/CD が npm install / gradle build を実行
6. 悪性コードが CI/CD 環境の secrets にアクセス
7. deploy credential や cloud credential が流出
```

#### パターンC：CI/CD workflow改ざん → secrets露出

```
1. third-party GitHub Action が侵害される
2. workflow がその Action を tag 参照で使用している
3. tag が差し替えられ、悪性コードを含むバージョンに更新される
4. 次回のCI実行で悪性コードが動作
5. secrets がログに出力される、または外部に送信される
```

### 4.4 なぜ「怪しいライブラリを入れない」では不十分なのか

上記の攻撃パターンを見ると、個人の注意だけでは防げないケースが多いことがわかる。

| 対策 | 限界 |
|------|------|
| 怪しいパッケージを入れない | 正規パッケージのmaintainerが侵害されたら区別できない |
| よく知らないパッケージは使わない | transitive dependencyは自分で選んでいない |
| install前にコードを読む | node_modulesに数千パッケージあるとき全部は読めない |
| dependencyの更新PRをよく見る | lockfileの差分数千行を毎回精読するのは現実的でない |

だからこそ、個人の注意ではなく**仕組み**で守る必要がある。

> **この章のまとめ**：自分たちの開発フローを攻撃面マップに当てはめると、個人の注意で防げる範囲はごく一部であることがわかる。次章では、その「仕組み」をどう作っていくかを示す。

---

## 5. 仕組みで守るという考え方

> **この章の目的**：「個人の注意」に代わる「仕組み」とは何かを、OWASP SCVSの管理領域と、この勉強会全体で作っていく仕組みの全体像として示す。

### 5.1 OWASP SCVSの管理領域

OWASP Software Component Verification Standard（SCVS）は、ソフトウェアサプライチェーンのリスクを減らすための活動・統制・ベストプラクティスを整理した標準である。

SCVSが示す管理領域：

| 領域 | 概要 |
|------|------|
| Inventory | 使用しているコンポーネントの一覧管理。「自分たちのアプリが、どのライブラリをいくつ使っているか」をそもそも把握していなければ、何かが侵害されたときに影響範囲も判断できない |
| SBOM | Software Bill of Materials（部品表）の生成と管理。依存関係の一覧を機械的に扱える形式で記録しておき、新しい脆弱性が公表されたときに「自分たちが影響を受けるか」を即座に確認できるようにする |
| Build Environment | ビルド環境の保護。CI/CDのビルドサーバー、依存関係のキャッシュ、ビルドに使うツール自体が改ざんされていないかを守る |
| Package Management | パッケージ管理の安全な運用。lockfileの利用、registryの設定、install scriptの実行制限など、依存関係を取得・更新する仕組みそのものを安全にする |
| Component Analysis | コンポーネントの脆弱性分析。使用しているライブラリに既知の脆弱性（CVE）が含まれていないかをスキャンし、把握する |
| Pedigree and Provenance | コンポーネントの出自（pedigree）と来歴（provenance）の検証。「このコードはどこから来て、誰が・どの環境でビルドしたものか」を確認できるようにする |

この勉強会では、これらの考え方をチームの現実的な開発フローに落とし込むことを目指す。

### 5.2 この勉強会で作る「仕組み」の概要

5回の勉強会を通じて、以下の仕組みをチームに導入することを目指す。

| 回 | 作る仕組み |
|----|-----------|
| 第1回 | 攻撃面マップ — 自分たちの開発フローのどこが攻撃対象かの共通認識 |
| 第2回 | Node系ライブラリ導入チェックリスト |
| 第3回 | Java/Kotlin依存関係レビュー観点 |
| 第4回 | CI/CD防御チェックリスト |
| 第5回 | ライブラリ汚染インシデント初動runbook |

> **この章のまとめ**：OWASP SCVSが示す管理領域を念頭に、5回の勉強会を通じて攻撃面マップ・チェックリスト・レビュー観点・CI/CD防御・初動runbookという仕組みを段階的に作っていく。

---

## 6. この勉強会全体の構成

> **この章の目的**：第1回から第5回までの構成と各回のつながりを示し、今回の位置づけと次回以降の見通しを持つ。

```
第1回 全体像（今回）
  └─ 何が起きているのかを知る
  └─ 攻撃面マップを共有する

第2回 Node / React / npm
  └─ フロントエンド開発に当てはめる
  └─ npm特有のリスクを理解する

第3回 Java / Kotlin / Gradle / Maven
  └─ バックエンド開発に当てはめる
  └─ JVM系特有のリスクを理解する

第4回 CI/CD・secrets・リリース経路
  └─ ライブラリ汚染の被害拡大経路を理解する
  └─ 権限分離と認証情報の最小化を学ぶ

第5回 インシデント対応と標準化
  └─ 踏んだ後の初動を整理する
  └─ チーム標準に落とし込む
```

> **この章のまとめ**：全体は「現状把握（第1回）→ フロントエンド／バックエンドそれぞれの実践（第2・3回）→ CI/CDと被害拡大経路（第4回）→ インシデント対応と標準化（第5回）」という流れで構成されている。

---

## 7. 第1回のまとめと次回への接続

> **この章の目的**：第1回全体で共有した内容を要点として整理し、次回（第2回）で扱う内容につなげる。

### この回で共有したこと

1. **依存関係はアプリケーションの一部である** — 外部ライブラリのコードは自分たちのプロダクトの一部として動作する
2. **攻撃対象は本番アプリだけではない** — 開発者端末、CI/CD、registry、認証情報まで攻撃対象が広がっている
3. **有名なライブラリだから安全とは言えない** — maintainerの侵害、publish tokenの流出、CI/CD workflowの悪用など、正規に見える経路が侵害される
4. **個人の注意ではなく仕組みで守る** — チェックリスト、権限分離、検知ツール、初動runbookなどの仕組みが必要

### 次回予告：第2回 Node / React / npm

次回は、Reactアプリケーション開発で日常的に使うnpmの世界に焦点を当てる。

- package.json、package-lock.jsonの読み方
- npm install時に何が起きるか
- install scriptのリスク
- transitive dependencyの確認方法
- npm audit、lockfile差分の見方
- Node系ライブラリ導入チェックリストの作成

---

## 確認問題

1. typosquattingとdependency confusionの違いを説明できるか
2. 自分たちの開発フローで、攻撃面マップのどの要素を使っているかを列挙できるか
3. 「有名なライブラリだから安全」が成り立たない理由を、具体的な攻撃手法を挙げて説明できるか
4. 個人の注意だけでは防げない理由を、transitive dependencyの観点から説明できるか
