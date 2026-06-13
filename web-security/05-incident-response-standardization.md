# 第5回：インシデント対応と標準化 — 踏んだ後にどう動くか

## この回の目的

ライブラリ汚染は完全に防ぐことが難しい。この回では、悪性パッケージを踏んだ後の初動を整理し、チームとして共通認識を作る。また、第1回〜第4回で作成した成果物をチームの開発標準に落とし込む。

---

## 1. なぜインシデント対応を扱うのか

### 1.1 完全防御が難しい理由

どれだけ注意していても、以下のケースは起こり得る。

| ケース | 防げない理由 |
|--------|------------|
| 正規パッケージのmaintainerが侵害される | パッケージ名もregistryも正規 |
| 正規の公開経路から悪性バージョンが出る | 通常の依存更新と区別できない |
| 依存先の依存先（transitive）が汚染される | 自分で選んだパッケージではない |
| merge済みのlockfileに問題が見つかる | すでにCI/CDと開発者端末で実行済み |
| CI cacheやartifactに悪性バージョンが残る | lockfileを修正してもcacheが汚染されたまま |
| 開発者端末でinstall済み | ローカル環境がすでに侵害されている可能性 |

### 1.2 初動の速さが被害を左右する

悪性パッケージを踏んだとき、最初に迷う時間が長いほど被害が広がる。

```
迷う時間の影響:
  - 漏洩した credential が使われ続ける
  - 汚染された cache から繰り返しビルドされる
  - 開発者端末から追加の情報が流出する
  - 他のリポジトリや環境に横展開される
  - 影響範囲の特定が難しくなる
```

事前にインシデント時の動き方を共有しておくことで、以下の判断を速くできる。

- CI/CDを止めるべきか
- どのtokenを失効すべきか
- どのログを見るべきか
- 開発者端末をどう扱うべきか
- どのバージョンに戻すべきか
- 誰に連絡すべきか

---

## 2. インシデント発見の経路

### 2.1 どこで気づくか

ライブラリ汚染のインシデントは、様々な経路で発見される。

| 発見経路 | 例 |
|---------|-----|
| 外部からの通知 | セキュリティアドバイザリ、CISA警告、ベンダー通知 |
| npm audit / SCAツール | 既知脆弱性として登録された場合 |
| GitHub Security Advisories | dependabot alertとして通知 |
| CI/CDの異常 | ビルドの異常な遅延、不審なネットワーク通信 |
| 開発者の気づき | lockfileの不自然な変更、見覚えのないパッケージ |
| cloud providerの通知 | 異常なAPI呼び出し、不審なリソース作成 |
| audit logの異常 | GitHub audit log、cloud audit logでの不審な操作 |
| SNS / セキュリティコミュニティ | X（旧Twitter）やセキュリティブログでの速報 |

### 2.2 外部通知を受け取る体制

能動的な監視だけでなく、外部からの通知を確実に受け取る体制が重要。

```
通知の受け取り体制:
  - GitHub Security Advisories / Dependabot alertsを有効にする
  - npm audit の定期実行とアラート
  - OWASP Dependency-Check の定期実行とアラート
  - セキュリティ関連のメーリングリストやRSSの購読
  - CISA、JVN などの公的機関の通知の購読
```

---

## 3. インシデント初動の流れ

### 3.1 全体の流れ

```
インシデント初動の流れ:

  [検知・通知]
    │
    ▼
  [1. 対象の特定]
    │  - 対象パッケージ名とバージョン
    │  - 影響する依存経路（direct / transitive）
    │
    ▼
  [2. 影響範囲の特定]
    │  - どのリポジトリが影響を受けるか
    │  - いつから影響を受けているか
    │  - CI/CDで実行されたか
    │  - 開発者端末でinstallされたか
    │
    ▼
  [3. 封じ込め]
    │  - CI/CD停止判断
    │  - credential失効・ローテーション
    │  - cache / artifact削除
    │  - lockfile rollback
    │
    ▼
  [4. 修復]
    │  - 安全なバージョンへのpinning
    │  - clean build
    │  - 開発者端末の対応
    │
    ▼
  [5. 確認]
    │  - audit logの確認
    │  - credentialの不正使用がないか確認
    │
    ▼
  [6. 報告・postmortem]
       - 関係者連絡
       - 時系列の整理
       - 再発防止策
```

---

## 4. 対象の特定

### 4.1 パッケージとバージョンの確認

```
確認すべき情報:
  - 悪性パッケージのexact name
  - 悪性バージョンの範囲（どのバージョンからどのバージョンまで）
  - 悪性コードが含まれた時期（公開日時）
  - 攻撃の種類（credential窃取、暗号通貨マイナー、バックドア等）
  - 悪性コードの動作タイミング（install時、ビルド時、実行時）
```

### 4.2 依存経路の確認

```bash
# npm: 対象パッケージがどの経路で入っているか
npm ls <package-name>

# npm: lockfileから対象バージョンを検索
grep -n "<package-name>" package-lock.json

# Gradle: 依存経路の確認
./gradlew dependencies | grep -B5 "<package-name>"

# Maven: 依存経路の確認
mvn dependency:tree | grep -B5 "<package-name>"
```

direct dependencyとして追加しているか、transitive dependencyとして入っているかで対応が異なる。

---

## 5. 影響範囲の特定

### 5.1 時間軸の特定

```
確認すべき時間軸:
  - 悪性バージョンがregistryに公開された日時
  - 自分たちのlockfileに悪性バージョンが入った日時
  - 最初にnpm ci / gradle buildが実行された日時（CI/CD）
  - 開発者がnpm install / gradle buildを実行した日時（端末）
  - 現在までの期間
```

```bash
# lockfileの変更履歴を確認（npm）
git log --oneline -p package-lock.json | grep -B5 "<package-name>"

# lockfileの変更履歴を確認（Gradle）
git log --oneline -p gradle.lockfile | grep -B5 "<package-name>"

# CI/CDの実行履歴を確認
gh run list --limit 50
```

### 5.2 影響を受けたリポジトリの特定

組織内の複数リポジトリが影響を受けている可能性がある。

```
確認観点:
  - 同じパッケージを使っているリポジトリはどれか
  - monorepoの場合、どのworkspaceが影響を受けるか
  - shared dependencyとして使われていないか
  - 社内パッケージが依存していないか
```

### 5.3 CI/CDでの実行確認

```
確認観点:
  - 悪性バージョンが含まれた状態でCI/CDが実行されたか
  - 実行されたworkflowでsecretsにアクセスできたか
  - ビルドartifactが生成されたか
  - deployが実行されたか
  - cacheに保存されたか
```

### 5.4 開発者端末の確認

```
確認観点:
  - 誰がnpm install / gradle buildを実行したか
  - そのタイミングで開発者端末に認証情報があったか
  - ~/.npmrc, ~/.gradle/gradle.properties, ~/.aws/credentials 等
  - 環境変数に認証情報が設定されていたか
```

---

## 6. 封じ込め

### 6.1 CI/CD停止判断

```
CI/CDを停止すべき状況:
  - 悪性パッケージがlockfileに残っている
  - cacheに悪性パッケージが含まれている可能性がある
  - workflowがsecretsにアクセスできる状態で悪性コードが実行される可能性がある

停止方法:
  - workflowの無効化
  - branch protectionで merge を一時停止
  - 該当リポジトリのCI/CD全体を一時停止
```

### 6.2 credential失効・ローテーション

**漏洩した可能性のあるcredentialは、確認を待たずに失効させる。** 「実際に漏洩したかどうか」の確認に時間をかけている間に悪用される可能性がある。

```
ローテーション対象の特定:

  CI/CD secrets:
    □ npm publish token
    □ GitHub PAT
    □ AWS credential (access key / secret key)
    □ GCP service account key
    □ Azure credential
    □ Docker registry credential
    □ signing key
    □ その他のAPI key

  開発者端末:
    □ ~/.npmrc の npm token
    □ ~/.gradle/gradle.properties の credential
    □ ~/.aws/credentials
    □ ~/.ssh/ の SSH鍵（GitHub等で使用）
    □ 環境変数に設定された credential
    □ GitHub PAT

  ローテーション手順:
    1. 現在のcredentialを失効させる
    2. 新しいcredentialを生成する
    3. 必要な場所に新しいcredentialを設定する
    4. 失効させたcredentialが使われていないことを確認する
```

### 6.3 cache / artifactの削除

```
削除対象:
  □ GitHub Actions cache
    - 該当する cache key を特定して削除
    - 必要に応じて全 cache を削除
  □ npm cache（開発者端末）
    - npm cache clean --force
  □ Gradle cache（開発者端末）
    - ~/.gradle/caches/ から該当するartifactを削除
  □ Maven cache（開発者端末）
    - ~/.m2/repository/ から該当するartifactを削除
  □ CI/CDのbuild artifact
    - 汚染された可能性のあるartifactを削除
  □ container registry
    - 汚染された可能性のあるimageを削除またはタグ解除
```

```bash
# GitHub Actions cacheの削除
gh cache list
gh cache delete <cache-key>

# npm cacheのクリア
npm cache clean --force

# node_modulesの削除
rm -rf node_modules

# Gradle cacheから特定のartifactを削除
find ~/.gradle/caches -path "*/<group-id>/<artifact-id>" -exec rm -rf {} +
```

### 6.4 lockfile rollback

```bash
# 悪性バージョンが入る前のlockfileに戻す
git log --oneline package-lock.json  # 変更履歴を確認

# 安全なcommitのlockfileを復元
git checkout <safe-commit-hash> -- package-lock.json

# clean install
rm -rf node_modules
npm ci
```

Gradleの場合：

```bash
# gradle.lockfileを安全な状態に戻す
git checkout <safe-commit-hash> -- gradle.lockfile

# verification-metadata.xmlも確認
git checkout <safe-commit-hash> -- gradle/verification-metadata.xml

# clean build
./gradlew clean build
```

---

## 7. 修復

### 7.1 安全なバージョンへのpinning

```json
// package.json: 安全なバージョンに固定
{
  "dependencies": {
    "affected-package": "1.2.3"  // exact version（^なし）
  },
  // overridesで transitive dependency を強制
  "overrides": {
    "affected-transitive-package": "1.0.0"
  }
}
```

```kotlin
// build.gradle.kts: バージョンの強制
configurations.all {
    resolutionStrategy {
        force("com.example:affected-lib:1.2.3")
    }
}
```

### 7.2 clean build

lockfileの修正後、clean buildを実施する。

```bash
# npm
rm -rf node_modules
npm ci
npm run build
npm test

# Gradle
./gradlew clean build

# Maven
mvn clean install
```

**cacheを使わずにビルドすることが重要。** CI/CDでは、cacheを削除した上でビルドを実行する。

### 7.3 開発者端末の対応

```
開発者端末の対応:
  1. node_modules / .gradle/caches / .m2/repository から該当パッケージを削除
  2. 認証情報のローテーション（前述）
  3. lockfileを安全なバージョンに更新
  4. clean install / clean build
  5. 必要に応じてマルウェアスキャン
```

開発者端末で悪性コードが実行された場合、端末自体が侵害されている可能性がある。悪性コードの内容によっては、OS再インストールが必要になる場合もある。ただし、多くの既知のサプライチェーン攻撃は、環境変数やファイルから認証情報を収集・送信するタイプであり、永続的なマルウェアを設置するケースは相対的に少ない。リスク判断はセキュリティチームと協議する。

---

## 8. 確認

### 8.1 audit logの確認

credentialが漏洩した可能性がある場合、audit logで不正使用がないか確認する。

#### GitHub audit log

```
確認ポイント:
  - 不審なrepository操作（clone, fork, settings変更）
  - 不審なbranch操作（新規branch作成, force push）
  - 不審なPAT作成やOAuthアプリ認可
  - 不審なmember追加やpermission変更
  - 不審なworkflow実行
```

```bash
# Organization audit logの確認（GitHub CLIまたはWeb UI）
# Web: https://github.com/organizations/<org>/settings/audit-log
```

#### Cloud audit log

```
AWS:
  - CloudTrail でAPIコール履歴を確認
  - 不審なリソース作成、IAM変更、データアクセス

GCP:
  - Cloud Audit Logs で確認
  - 不審なリソース作成、IAM変更

Azure:
  - Azure Activity Log で確認
  - 不審なリソース操作
```

#### npm audit log

```
npm:
  - npmアカウントのaccess token一覧を確認
  - 不審なtoken作成がないか
  - パッケージのバージョン公開履歴を確認
```

### 8.2 不正使用の痕跡

```
確認すべき痕跡:
  □ 見覚えのないcommitやPR
  □ 見覚えのないbranch
  □ 見覚えのないrelease / tag
  □ 見覚えのないpackage version公開
  □ 見覚えのないcloud resource
  □ 見覚えのないIAM user / role / policy変更
  □ 見覚えのないdata access
  □ 見覚えのないnetwork設定変更
  □ 見覚えのないDNS変更
```

---

## 9. 報告・postmortem

### 9.1 関係者への連絡

```
連絡先の例:
  □ チームメンバー（全員）
  □ セキュリティチーム / CSIRT
  □ インフラチーム / SRE
  □ 影響を受けたサービスの関係者
  □ マネージャー / プロダクトオーナー
  □ 必要に応じて法務・コンプライアンス
  □ 必要に応じて顧客・利用者への通知
```

### 9.2 連絡に含める情報

```
インシデント連絡の内容:
  - 何が起きたか（対象パッケージ、攻撃の種類）
  - いつからいつまで影響があったか
  - 影響範囲（リポジトリ、環境、credential）
  - 現在の状態（封じ込め済み / 修復中 / 確認中）
  - 実施済みの対応
  - 今後の対応予定
  - 各自に求めるアクション（credential変更等）
```

### 9.3 postmortem

インシデント対応が落ち着いた後、postmortemを実施する。

```
postmortemの構成:

  1. 概要
     - 何が起きたか
     - 影響範囲
     - 対応期間

  2. 時系列
     - 悪性バージョンの公開日時
     - 自分たちのlockfileに入った日時
     - CI/CDで実行された日時
     - 検知日時
     - 封じ込め完了日時
     - 修復完了日時

  3. 影響
     - 漏洩した可能性のあるcredential
     - 影響を受けたリポジトリ / 環境
     - 不正使用の有無

  4. 対応内容
     - 実施した封じ込め
     - 実施した修復
     - 実施した確認

  5. 根本原因分析
     - なぜ悪性パッケージが入ったか
     - なぜ検知に時間がかかったか
     - なぜ影響が広がったか

  6. 再発防止策
     - 検知の改善
     - 防御の改善
     - 対応フローの改善

  7. 教訓
     - 今回うまくいったこと
     - 今回うまくいかなかったこと
     - チームとして学んだこと
```

---

## 10. ライブラリ汚染インシデント初動runbook

この回の成果物として、以下のrunbookを共有する。チームの状況に合わせてカスタマイズして使用する。

### フェーズ1：検知・対象特定（目標：30分以内）

```
□ 対象パッケージ名とバージョンを特定する
□ 悪性コードの動作を把握する（credential窃取、マイナー、バックドア等）
□ 悪性バージョンの公開時期を把握する
□ direct dependency か transitive dependency かを確認する
□ 影響を受けるリポジトリを特定する
□ チーム内に第一報を共有する
```

### フェーズ2：影響範囲特定（目標：1時間以内）

```
□ lockfileの履歴から、悪性バージョンが入った日時を特定する
□ CI/CDの実行履歴から、悪性コードが実行された日時を特定する
□ CI/CDでsecretsにアクセスできたかを確認する
□ 開発者端末でinstallした人を確認する
□ deployが実行されたか確認する
□ cacheに保存されているか確認する
```

### フェーズ3：封じ込め（目標：影響範囲特定後すみやかに）

```
□ 漏洩した可能性のあるcredentialを失効させる
  □ npm token
  □ GitHub PAT
  □ cloud credential（AWS / GCP / Azure）
  □ CI/CD secrets
  □ その他のAPI key
□ CI/CDを一時停止する（必要な場合）
□ cacheを削除する
  □ GitHub Actions cache
  □ 開発者端末のnpm / Gradle / Maven cache
□ 汚染された可能性のあるartifactを削除する
```

### フェーズ4：修復

```
□ lockfileを安全なバージョンにrollbackする
□ 安全なバージョンにpinningする（必要な場合overridesも）
□ clean build を実施する（cacheなし）
□ テストを実行して正常動作を確認する
□ 新しいcredentialを生成・設定する
□ CI/CDを再開する
□ 開発者にclean install / clean buildを指示する
```

### フェーズ5：確認

```
□ GitHub audit logで不正操作がないか確認する
□ cloud audit logで不正操作がないか確認する
□ npmアカウントの不審なtoken作成がないか確認する
□ 不審なcommit、PR、release、tagがないか確認する
□ 不審なcloud resource作成がないか確認する
```

### フェーズ6：報告・postmortem

```
□ 関係者に対応状況を報告する
□ セキュリティチームに報告する（必要な場合）
□ 時系列を整理する
□ postmortemを実施する
□ 再発防止策を決定する
□ 再発防止策を実施する
```

---

## 11. 第1回〜第5回の成果物をチーム標準に落とし込む

### 11.1 各回の成果物一覧

| 回 | 成果物 | 用途 |
|----|--------|------|
| 第1回 | 攻撃面マップ | 全員の共通認識 |
| 第2回 | Node系ライブラリ導入チェックリスト | パッケージ追加・更新PRのレビュー |
| 第3回 | Java/Kotlin依存関係レビュー観点 | 依存関係・ビルド設定の変更PRのレビュー |
| 第4回 | CI/CD防御チェックリスト | CI/CD設定のレビュー・改善 |
| 第5回 | インシデント初動runbook | インシデント発生時の対応 |

### 11.2 チーム標準への統合

#### PRテンプレートへの反映

```markdown
## 依存関係の変更がある場合
- [ ] パッケージ名にtypoがないか確認した
- [ ] install script / Gradle plugin / annotation processorの有無を確認した
- [ ] lockfile差分を確認した
- [ ] 新しいrepositoryが追加されていないことを確認した
- [ ] npm audit / SCAツールで既知脆弱性を確認した
```

#### CI/CDの設定改善

```
改善項目:
  □ workflowのpermissionsを明示する
  □ デフォルトpermissionsをread-onlyにする
  □ test jobとdeploy jobを分離する
  □ environment protectionを設定する
  □ secretsのスコープを見直す
  □ OIDCの導入を検討する
  □ third-party actionsをcommit SHAで固定する
  □ Gradle Wrapper / Maven Wrapper の検証をCIに追加する
  □ SCAツールをCIに組み込む
```

#### インシデント対応体制の整備

```
整備項目:
  □ runbookをチームのドキュメントとして保存する
  □ 連絡経路を明文化する
  □ credentialのローテーション手順を整理する
  □ 定期的なrunbookの見直しをスケジュールする
```

### 11.3 段階的な導入

すべてを一度に導入する必要はない。優先度をつけて段階的に進める。

```
優先度高（すぐに実施）:
  - CI/CDのpermissionsを明示する
  - secretsのスコープを見直す
  - インシデント時の連絡経路を明文化する

優先度中（1〜2ヶ月以内）:
  - PRテンプレートにチェックリストを追加する
  - test jobとdeploy jobを分離する
  - SCAツールをCIに組み込む
  - third-party actionsをcommit SHAで固定する

優先度低（3ヶ月以内に検討開始）:
  - OIDCの導入
  - Trusted Publishingへの移行
  - Dependency Verificationの導入
  - credential棚卸しの定期実施
```

---

## 12. 勉強会全体のまとめ

### 5回で学んだこと

```
第1回：全体像
  - ライブラリ汚染はOSSサプライチェーン攻撃の一形態
  - 攻撃対象は本番アプリだけでなく開発・ビルド・リリースプロセス全体
  - 個人の注意ではなく仕組みで守る

第2回：Node / React / npm
  - npm install時のlifecycle scriptによるコード実行リスク
  - lockfileの重要性と差分の見方
  - npm audit, provenance, Trusted Publishing
  - パッケージ追加時のチェックリスト

第3回：Java / Kotlin / Gradle / Maven
  - Gradle plugin, Maven plugin, annotation processorのビルド時実行リスク
  - repository設定、SNAPSHOT、動的バージョンの問題
  - Dependency Verificationによる検証
  - OWASP Dependency-Checkの活用

第4回：CI/CD・secrets・リリース経路
  - CI/CDが攻撃者にとって価値の高い理由
  - permissionsの明示と最小化
  - deploy jobとtest jobの分離
  - OIDCによる長期token削減
  - third-party actionsのcommit SHA固定

第5回：インシデント対応と標準化
  - 完全防御が難しいため初動runbookが必要
  - credential失効を最優先にする
  - audit logの確認方法
  - postmortemによる改善サイクル
  - チーム標準への段階的な落とし込み
```

### 成功条件の確認

#### 最低限達成したい状態

```
□ 参加者がライブラリ汚染の基本的な攻撃パターンを説明できる
□ パッケージ追加や依存更新PRのレビュー観点が共有されている
□ Node系とJVM系でリスクの見え方が違うことを理解している
□ CI/CDに不要な権限やsecretsを置かない重要性を理解している
□ 悪性パッケージ混入時の初動runbookの叩き台がある
```

#### 可能なら達成したい状態

```
□ 依存追加チェックリストが実際のPRテンプレートに反映される
□ CI/CDのpermissionsが明示される
□ deploy credentialやpublish tokenの棚卸しが行われる
□ OIDCやTrusted Publishingへの移行検討が始まる
□ Dependency VerificationやSCAツールの導入・改善が検討される
□ インシデント時の連絡経路が明文化される
```

---

## 確認問題

1. 悪性パッケージを踏んだことが判明した場合、最初にすべきことは何か
2. credentialの不正使用を確認するために、どのようなログを確認すべきか
3. lockfile rollback後にclean buildが必要な理由を説明できるか
4. CI/CDのcacheを削除すべき理由を、cache poisoningの観点から説明できるか
5. postmortemで根本原因分析を行う意義を説明できるか
6. チーム標準への落とし込みで、最も優先度の高い項目は何か。その理由は
