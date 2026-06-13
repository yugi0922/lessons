# 第1回：ライブラリ汚染の全体像 — 攻撃面マップを共有する

## この回の目的

ライブラリ汚染を「怪しいパッケージに注意しよう」という個人の心がけではなく、OSSサプライチェーン攻撃として捉え、開発プロセス全体のどこが攻撃対象になるかを全員で共有する。

第2回以降の技術的な話に入る前に、フロントエンド担当もバックエンド担当も、ジュニアもシニアも、同じ地図を持つことがこの回のゴールである。

---

## 1. 現代のWebアプリケーション開発と依存関係

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

---

## 2. 攻撃対象は本番アプリだけではない

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

---

## 3. ライブラリ汚染の攻撃パターン

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

maintainerのpublish tokenがGitHub repositoryやCI/CDログに露出し、攻撃者がそのtokenでパッケージを公開する。

#### CI/CD workflowの悪用

GitHub ActionsなどのCI/CD workflowを改ざんし、ビルドやリリースの過程で悪性コードを注入する。

#### registryや配布物の差し替え

registryインフラ自体を侵害するか、registryとクライアントの間で配布物を差し替える。

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

---

## 4. 攻撃面マップ — 自分たちの開発フローを整理する

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

### 4.2 攻撃の流れの例

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

### 4.3 なぜ「怪しいライブラリを入れない」では不十分なのか

上記の攻撃パターンを見ると、個人の注意だけでは防げないケースが多いことがわかる。

| 対策 | 限界 |
|------|------|
| 怪しいパッケージを入れない | 正規パッケージのmaintainerが侵害されたら区別できない |
| よく知らないパッケージは使わない | transitive dependencyは自分で選んでいない |
| install前にコードを読む | node_modulesに数千パッケージあるとき全部は読めない |
| dependencyの更新PRをよく見る | lockfileの差分数千行を毎回精読するのは現実的でない |

だからこそ、個人の注意ではなく**仕組み**で守る必要がある。

---

## 5. 仕組みで守るという考え方

### 5.1 OWASP SCVSの管理領域

OWASP Software Component Verification Standard（SCVS）は、ソフトウェアサプライチェーンのリスクを減らすための活動・統制・ベストプラクティスを整理した標準である。

SCVSが示す管理領域：

| 領域 | 概要 |
|------|------|
| Inventory | 使用しているコンポーネントの一覧管理 |
| SBOM | Software Bill of Materialsの生成と管理 |
| Build Environment | ビルド環境の保護 |
| Package Management | パッケージ管理の安全な運用 |
| Component Analysis | コンポーネントの脆弱性分析 |
| Pedigree and Provenance | コンポーネントの出自と来歴の検証 |

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

---

## 6. この勉強会全体の構成

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

---

## 7. 第1回のまとめと次回への接続

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
