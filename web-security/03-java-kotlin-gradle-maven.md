# 第3回：Java / Kotlin / Gradle / Maven — バックエンド開発のサプライチェーンリスク

## この回の目的

Java / Kotlin開発者が日常的に扱うGradle・Mavenの依存関係管理を題材に、JVM系特有のサプライチェーンリスクを理解する。npmとの違いを明確にしたうえで、build.gradle.ktsやpom.xmlに当てはめて考えられるようにし、Java/Kotlin依存関係レビュー観点を作成する。

---

## 1. npmとJVM系のリスクの違い

### 1.1 install時のコード実行

| 項目 | npm | Gradle / Maven |
|------|-----|---------------|
| install時のスクリプト実行 | あり（lifecycle scripts） | 通常のlibrary dependencyではなし |
| ビルド時のコード実行 | webpack / Vite等のビルドツール | Gradle plugin, Maven plugin, annotation processor |
| 攻撃のタイミング | `npm install`時 | `gradle build` / `mvn compile`時 |

JVM系では、通常のlibrary dependency（`implementation`や`<dependency>`で追加するもの）がinstall時にスクリプトを実行することはない。

**しかし、Gradle plugin、Maven plugin、annotation processorは、ビルド時にコードを実行する。** これらはnpmのinstall scriptとは異なる経路だが、同等以上のリスクを持つ。

### 1.2 リスクの所在の違い

```
npm系のリスク:
  npm install → install script実行 → 認証情報窃取
  ↑ 依存追加のタイミングで攻撃が成立

JVM系のリスク:
  gradle build / mvn compile → plugin実行 → 認証情報窃取
  gradle build / mvn compile → annotation processor実行 → コード生成時に悪性コード注入
  ↑ ビルドのタイミングで攻撃が成立
```

npmでは「install時に何が実行されるか」が焦点だが、JVM系では「ビルド時に何が実行されるか」が焦点になる。

---

## 2. Gradle pluginのリスク

### 2.1 Gradle pluginとは何か

Gradle pluginは、ビルドプロセスを拡張するためのコンポーネントである。

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.spring") version "2.0.0"
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("com.google.protobuf") version "0.9.4"
}
```

**Gradle pluginは、ビルドスクリプトのクラスパスに入り、ビルドプロセス中に任意のコードを実行できる。** これはnpmのdevDependenciesとは比較にならないほど強い権限を持つ。

### 2.2 pluginが持つ権限

Gradle pluginは以下のことが可能である。

- ファイルシステムへの読み書き
- 環境変数の参照
- ネットワークアクセス
- プロセスの起動
- Gradleのビルド設定の変更
- 他のtaskの追加・変更

```
Gradle pluginが実行される流れ:
  1. gradle build を実行
  2. build.gradle.kts を評価
  3. plugins ブロックで指定された plugin を取得
  4. plugin のコードが実行される ← ここで任意コード実行が可能
  5. 通常のビルドが進行
```

悪性のGradle pluginは、ビルド時に環境変数や`~/.gradle/gradle.properties`の認証情報を読み取り、外部に送信できる。

### 2.3 Gradle plugin追加は通常のdependency追加より厳しく見るべき理由

```kotlin
// 通常のdependency — 実行時にアプリケーションコードから使う
dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
}

// Gradle plugin — ビルド時にビルドプロセス自体の権限で動作する
plugins {
    id("some-gradle-plugin") version "1.0.0"
}
```

| 観点 | 通常のdependency | Gradle plugin |
|------|-----------------|---------------|
| 実行タイミング | アプリケーション実行時 | ビルド時 |
| 実行環境 | アプリケーションのサンドボックス内 | ビルドプロセス（制限なし） |
| アクセスできるもの | アプリケーションに渡された情報 | ファイルシステム、環境変数、ネットワーク |
| CI/CDでの影響 | deployされたアプリに限定 | CI/CD環境のsecrets全体 |

**Gradle pluginの追加は、通常のlibrary dependencyの追加より厳格にレビューすべきである。**

---

## 3. Maven pluginのリスク

### 3.1 Maven pluginの権限

Maven pluginも、ビルドのライフサイクルにバインドされ、ビルド時にコードを実行する。

```xml
<!-- pom.xml -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
        </plugin>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>3.3.0</version>
        </plugin>
    </plugins>
</build>
```

Maven pluginはビルドの各フェーズ（compile, test, package, install, deploy）にバインドされ、そのフェーズで任意のコードを実行できる。

### 3.2 Maven Wrapperの安全性

Maven WrapperはGradle Wrapperと同様に、プロジェクトに必要なMavenバージョンを固定し自動ダウンロードする仕組みである。

```
.mvn/
  wrapper/
    maven-wrapper.properties  ← ダウンロード元URL
```

`maven-wrapper.properties`のダウンロードURLが改ざんされると、悪性のMavenバイナリが取得される可能性がある。Gradle Wrapperについても同様のリスクがある。

---

## 4. annotation processorのリスク

### 4.1 annotation processorとは

annotation processorは、コンパイル時にアノテーションを解析してコードを生成する仕組みである。

```kotlin
// build.gradle.kts
dependencies {
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")
    kapt("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
}
```

代表的なannotation processor：

| ツール | 用途 |
|--------|------|
| Lombok | ボイラープレートコード生成 |
| MapStruct | DTO変換コード生成 |
| Dagger/Hilt | DI（依存性注入）コード生成 |
| Room (Android) | データベースアクセスコード生成 |
| KSP (Kotlin Symbol Processing) | Kotlin向けコード生成 |

### 4.2 annotation processorが持つ権限

annotation processorは**コンパイラのプロセス内で動作する**。

```
annotation processorの実行環境:
  - コンパイラプロセス内で動作
  - ソースコード全体にアクセス可能
  - ファイルシステムへの読み書きが可能
  - 新しいソースファイルを生成可能
  - 環境変数にアクセス可能
```

悪性のannotation processorが含まれた場合、コンパイル時に以下が可能：

- ソースコードの読み取り（知的財産の窃取）
- 生成コードへの悪性コードの注入
- 環境変数や認証情報の窃取
- ファイルシステムへのアクセス

**annotation processorの追加も、Gradle pluginと同様に厳格にレビューすべきである。**

---

## 5. repository設定のリスク

### 5.1 Gradleのrepository設定

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    google()
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        url = uri("https://some-company-repo.example.com/maven")
    }
    mavenLocal()
}
```

repository設定は、依存関係をどこから取得するかを決める。

### 5.2 repositoryに関するリスク

#### 信頼できないrepositoryの追加

PRで新しいrepositoryが追加されている場合、そのrepositoryの信頼性を確認する必要がある。

```kotlin
// 要確認：なぜこのrepositoryが必要か
maven {
    url = uri("https://unknown-repo.example.com/maven")
}
```

Gradle公式ドキュメントでは、repository shadowingのリスクに触れている。複数のrepositoryが設定されている場合、上位のrepositoryに同名・同バージョンのartifactを配置することで、正規のartifactを差し替えることが可能になる。

#### mavenLocalのリスク

`mavenLocal()`は、開発者端末の`~/.m2/repository`を参照する。

```
mavenLocal()のリスク:
  - ローカルにキャッシュされたartifactが改ざんされている可能性
  - ローカルの状態に依存するため、再現性がない
  - CI/CDでmavenLocal()を使うと、ビルドの再現性と安全性が低下する
```

**CI/CDではmavenLocal()を使うべきではない。** 開発時であっても、必要な場合を除き使用を避ける。

#### name squatting

Gradle公式ドキュメントでは、name squattingのリスクにも触れている。Maven CentralやGradle Plugin Portalに、既存の有名なパッケージに似た名前のartifactを公開する攻撃である。npmのtyposquattingと同様のリスクがJVM系にも存在する。

### 5.3 Mavenのrepository設定

```xml
<!-- pom.xml -->
<repositories>
    <repository>
        <id>central</id>
        <url>https://repo.maven.apache.org/maven2</url>
    </repository>
    <repository>
        <id>company-repo</id>
        <url>https://some-company-repo.example.com/maven</url>
    </repository>
</repositories>
```

Mavenでも同様に、信頼できないrepositoryの追加や、repository設定の変更には注意が必要である。

---

## 6. SNAPSHOTと動的バージョン

### 6.1 SNAPSHOT dependencyのリスク

```kotlin
// SNAPSHOT version
dependencies {
    implementation("com.example:some-lib:1.0.0-SNAPSHOT")
}
```

SNAPSHOT versionは、同じバージョン番号でも中身が変わる可能性がある。

```
SNAPSHOT の動作:
  1.0.0-SNAPSHOT を依存に追加
  → 取得するたびに最新のSNAPSHOT artifactを確認
  → 同じバージョン番号でも中身が異なる可能性がある
  → ビルドの再現性がない
```

**攻撃者がSNAPSHOT repositoryに書き込めれば、同じバージョン番号のまま悪性artifactに差し替えられる。**

### 6.2 動的バージョンのリスク

```kotlin
// 動的バージョン指定（危険）
dependencies {
    implementation("com.example:some-lib:1.+")
    implementation("com.example:some-lib:latest.release")
}
```

動的バージョン指定は、解決のたびに異なるバージョンが選択される可能性がある。lockfileで固定していない場合、攻撃者がバージョン範囲内の新バージョンを公開するだけで取り込まれる。

---

## 7. Dependency Verification

### 7.1 GradleのDependency Verification

GradleのDependency Verificationは、依存成果物やmetadata、pluginなどについてchecksumやsignatureを用いて検証する仕組みを提供している。

```
gradle/
  verification-metadata.xml  ← checksumやsignatureの情報
```

#### checksum verification

```xml
<!-- gradle/verification-metadata.xml -->
<verification-metadata>
    <components>
        <component group="com.fasterxml.jackson.core" 
                   name="jackson-databind" 
                   version="2.17.0">
            <artifact name="jackson-databind-2.17.0.jar">
                <sha256 value="abc123..." origin="Maven Central"/>
            </artifact>
        </component>
    </components>
</verification-metadata>
```

checksumを記録しておくことで、依存成果物が差し替えられた場合に検出できる。

#### signature verification

PGP署名による検証も可能である。artifactの署名を検証することで、正規のmaintainerが公開したものであることを確認できる。

### 7.2 Dependency Verificationの導入

```bash
# verification-metadata.xml の生成
./gradlew --write-verification-metadata sha256

# 署名検証も含める場合
./gradlew --write-verification-metadata sha256,pgp
```

生成されたverification-metadata.xmlをリポジトリにcommitし、依存関係の更新時に差分を確認する。

**新しい依存関係を追加したとき、verification-metadata.xmlに新しいエントリが追加される。このエントリが正当なものであることを確認する。**

### 7.3 Mavenでの依存検証

Mavenには、Gradleほど統合されたDependency Verification機能はないが、以下の方法で検証できる。

- maven-enforcer-pluginで依存関係の制約を設定する
- checksumの検証（Mavenはデフォルトでchecksumを検証する）
- `<repositories>`の設定でHTTPS通信を強制する

---

## 8. SCAツール — OWASP Dependency-Check

### 8.1 OWASP Dependency-Checkとは

OWASP Dependency-Checkは、プロジェクトの依存関係を分析し、CVEなどの既知脆弱性に関連づけてレポートするSCAツールである。Maven plugin、Gradle plugin、CLI、Jenkins、GitHub Actions、Azure DevOpsなどの連携が提供されている。

### 8.2 Gradleでの導入

```kotlin
// build.gradle.kts
plugins {
    id("org.owasp.dependencycheck") version "10.0.0"
}

dependencyCheck {
    failBuildOnCVSS = 7.0f  // CVSS 7.0以上で失敗
    formats = listOf("HTML", "JSON")
}
```

```bash
# 脆弱性チェックの実行
./gradlew dependencyCheckAnalyze
```

### 8.3 Mavenでの導入

```xml
<!-- pom.xml -->
<build>
    <plugins>
        <plugin>
            <groupId>org.owasp</groupId>
            <artifactId>dependency-check-maven</artifactId>
            <version>10.0.0</version>
            <configuration>
                <failBuildOnCVSS>7</failBuildOnCVSS>
            </configuration>
        </plugin>
    </plugins>
</build>
```

```bash
# 脆弱性チェックの実行
mvn dependency-check:check
```

### 8.4 SCAツールの限界

npm auditと同様に、SCAツールは**既知の脆弱性**のみを検出する。

- 意図的に仕込まれた悪性コード（CVEが割り振られていない）は検出できない
- 0-day脆弱性は検出できない
- false positive（誤検知）も発生する

SCAツールは重要な防御層だが、それだけでは不十分であることを理解しておく。

---

## 9. Gradle / Maven固有の追加観点

### 9.1 buildSrcとincluded build

```
buildSrc/
  src/main/kotlin/
    convention-plugins.gradle.kts  ← ビルドロジックのカスタマイズ
```

buildSrcやincluded buildは、プロジェクト固有のビルドロジックを定義する場所である。ここに外部依存関係を追加する場合、それはビルドプロセスの一部として動作する。

### 9.2 Gradle Wrapperの安全性

```
gradle/
  wrapper/
    gradle-wrapper.jar         ← Gradleのダウンロードと起動を行うJAR
    gradle-wrapper.properties  ← ダウンロード元URL、バージョン
```

`gradle-wrapper.jar`はリポジトリにcommitされるバイナリファイルである。このファイルが改ざんされると、悪性のGradleが実行される。

**Gradle Wrapper JARの正当性は確認すべきである。** Gradleは公式にWrapper JARの検証方法を提供している。

```bash
# Gradle Wrapper JARの検証
./gradlew wrapper --gradle-version=8.8 --verify
```

GitHub Actionsでは、`gradle/actions/wrapper-validation`アクションを使ってWrapper JARの正当性を検証できる。

### 9.3 settings.gradle.ktsのpluginManagement

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://some-repo.example.com/")
        }
    }
}
```

pluginManagementのrepository設定も、通常のrepository設定と同様にレビュー対象である。

---

## 10. Java/Kotlin依存関係レビュー観点

この回の成果物として、以下のレビュー観点を共有する。

### 依存関係の追加・更新時

```
□ 新しいrepositoryが追加されていないか
  - 追加されている場合、そのrepositoryの信頼性を確認したか
□ mavenLocal()がCI/CDで使われていないか
□ SNAPSHOT versionや動的バージョンを使っていないか
□ Gradle plugin追加を通常のdependencyより厳格にレビューしたか
  - pluginの提供元は信頼できるか
  - Gradle Plugin Portalでの情報を確認したか
□ annotation processor追加を確認したか
  - kaptやannotationProcessorの追加は厳格にレビュー
□ Dependency Verificationの対象か
  - verification-metadata.xmlに新しいエントリが追加されているか
  - エントリが正当か確認したか
□ OWASP Dependency-Check等のSCAツールで既知脆弱性を確認したか
□ transitive dependencyの変化を確認したか
```

### ビルド設定のレビュー

```
□ build.gradle.kts / pom.xml の変更に不要なrepository追加がないか
□ settings.gradle.kts の pluginManagement 変更を確認したか
□ buildSrc / included build の依存関係変更を確認したか
□ Gradle Wrapper / Maven Wrapper のバージョン変更を確認したか
□ Gradle Wrapper JAR の正当性を検証したか
```

### CI/CDでのGradle/Maven運用

```
□ CI/CDでmavenLocal()を使っていないか
□ SNAPSHOT dependencyをCI/CDで使っていないか
□ dependency cacheの安全性を確認しているか
□ Dependency Verificationを有効にしているか
□ SCAツールをCIに組み込んでいるか
□ Gradle Wrapperの検証をCIに組み込んでいるか
```

---

## 11. まとめと次回への接続

### この回で学んだこと

1. **npmとJVM系のリスクの違い** — install時ではなくビルド時に攻撃が成立する
2. **Gradle plugin / Maven pluginの権限** — ビルドプロセス内で任意コード実行が可能
3. **annotation processorのリスク** — コンパイラプロセス内で動作し、広範な権限を持つ
4. **repository設定のリスク** — 信頼できないrepository、mavenLocal、repository shadowingの問題
5. **SNAPSHOTと動的バージョンのリスク** — ビルドの再現性と安全性の問題
6. **Dependency Verification** — checksumやsignatureによる依存成果物の検証
7. **SCAツール** — OWASP Dependency-Checkによる既知脆弱性の検出

### 次回予告：第4回 CI/CD・secrets・リリース経路

次回は、第2回・第3回で学んだnpm系・JVM系のリスクが、CI/CD上でどのように被害拡大するかを扱う。

- CI/CDが攻撃者にとって価値の高い理由
- GitHub Actions workflowのpermissions
- write権限の最小化
- deploy jobとtest jobの分離
- secretsのスコープ管理
- OIDCによる長期token削減
- third-party actionsの固定方法
- CI/CD防御チェックリストの作成

---

## 確認問題

1. npmのinstall scriptとGradle pluginのリスクの違いを説明できるか
2. Gradle pluginの追加を通常のdependencyの追加より厳格にレビューすべき理由を説明できるか
3. annotation processorが持つ権限と、そのリスクを説明できるか
4. mavenLocal()をCI/CDで使うべきでない理由を説明できるか
5. Dependency Verificationの目的と仕組みを説明できるか
6. SCAツールの限界を説明できるか
