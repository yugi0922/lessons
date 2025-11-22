# コルーチン入門：非同期処理の新しいアプローチ

## 導入：非同期処理の課題とコルーチンの誕生

### なぜ非同期処理は難しいのか

アプリケーションでは「同時に複数の処理を行いたい」場面がよくあります。
たとえば、

* 画面を表示しながらネットワーク通信をする
* 複数のAPIを同時に呼び出して、すべての結果をまとめる
* 大きなファイルを読み込みながら、ユーザー入力を受け付ける

このような「並行処理」を実現するために、Javaでは長い間 **Thread（スレッド）** が使われてきました。

---

### Javaの典型的な非同期処理の形

Javaでは以下のような方法が使われます。

* `new Thread { ... }`
  → 手動でスレッドを起動
* `ExecutorService.submit()`
  → スレッドプールを使ってタスクを実行
* `CompletableFuture`
  → 非同期処理をチェーンでつなぐ

例：`CompletableFuture`

```java
CompletableFuture.supplyAsync(() -> fetchData())
    .thenApply(data -> process(data))
    .thenAccept(result -> System.out.println(result));
```

* 例外処理が複雑
* スレッドプールの設定が難しい
* ネストが増えると読みにくい
  という課題が残ります。


### Threadによる並行処理の限界

ThreadはOSが直接管理する仕組みです。
そのため、1つ作るだけでもかなりのメモリ（数百KB〜数MB）を消費し、数千個も立ち上げるのは非現実的です。

また、スレッドを自分で管理するコードは複雑になりがちです。

```java
new Thread(() -> {
    try {
        var result = fetchData();
        System.out.println(result);
    } catch (Exception e) {
        e.printStackTrace();
    }
}).start();
```

スレッドの中で例外処理や完了後のコールバックを自分で書く必要があります。
これが増えていくと「**コールバック地獄（callback hell）**」が発生します。

もちろんです。上記のコールバック地獄の解説とコードを、Markdownファイルとして整形します。

-----

## 💥 コールバック地獄の具体例

スレッドの中で例外処理や完了後のコールバックを自分で書く必要があります。これが**増えていくと**、処理がどんどん入れ子になり、\*\*「コールバック地獄（callback hell）」\*\*が発生します。

この例では、「データ取得」→「データ加工」→「結果保存」という3つの非同期処理が連続しています。

```java
// 非同期処理の完了後に「次に実行する処理」を定義するためのインターフェース
interface Callback {
    void onComplete(String result); // 成功した時に呼ばれる
    void onError(Exception e);     // 失敗した時に呼ばれる
}

// ----------------------------------------------------------------------

// 処理1: データを非同期で取得する処理を定義
AsyncOperation fetchData = (callback) -> {
    new Thread(() -> {
        try {
            // ... データを取得する処理（時間がかかる作業） ...
            Thread.sleep(100); 
            String rawData = "Original Data: [1, 2, 3]";
            System.out.println("Step 1: データを取得しました -> " + rawData);
            
            // 成功！次の処理（コールバック）を呼び出す
            callback.onComplete(rawData); 
        } catch (Exception e) {
            callback.onError(e);
        }
    }).start();
};

// ----------------------------------------------------------------------

// メインの処理実行
System.out.println("非同期処理を開始します...");

// ここからコールバック地獄の始まり...
fetchData.execute(new Callback() { // <<< 最初の非同期処理の開始
    @Override
    public void onComplete(String rawData) {
        
        // --- Step 2: 取得したデータを非同期で加工する ---
        System.out.println("Step 1完了。Step 2（加工処理）を開始...");
        
        new Thread(() -> {
            try {
                // ... データを加工する処理（時間がかかる作業） ...
                Thread.sleep(100); 
                String processedData = rawData.replace("[", "<").replace("]", ">");
                System.out.println("Step 2: データを加工しました -> " + processedData);
                
                // 成功！次の処理（コールバック）を呼び出すために、さらにコールバックを定義
                new Callback() { // <<< コールバックの中に、さらに次の処理（コールバック）を記述
                    @Override
                    public void onComplete(String dataToSave) {
                        
                        // --- Step 3: 加工したデータを非同期で保存する ---
                        System.out.println("Step 2完了。Step 3（保存処理）を開始...");
                        
                        new Thread(() -> {
                            try {
                                // ... データを保存する処理（時間がかかる作業） ...
                                Thread.sleep(100);
                                String finalResult = "Saved: " + dataToSave;
                                System.out.println("Step 3: データを保存しました -> " + finalResult);
                                System.out.println("全ての非同期処理が完了しました。");
                            } catch (Exception e) {
                                System.err.println("Step 3でエラーが発生しました: " + e.getMessage());
                            }
                        }).start();
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        // Step 3のエラー処理
                    }
                }.onComplete(processedData); // Step 3のコールバック呼び出し
                
            } catch (Exception e) {
                System.err.println("Step 2でエラーが発生しました: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void onError(Exception e) {
        System.err.println("Step 1でエラーが発生しました: " + e.getMessage());
    }
});
```

-----

## 🤔 なぜ「地獄」なのか？

上記のコードのように、非同期処理が連続するたびにコードが\*\*内側へ、内側へと深く入れ子（ネスト）\*\*になっていきます。

  * コードの\*\*インデント（字下げ）\*\*がどんどん深くなり、処理の全体像が見えにくい。
  * どの処理がどの処理の**完了後**に実行されているのか、**処理の流れを追うのが非常に困難**になる。
  * 処理の追加や変更が難しくなり、**バグの原因**になりやすい。

これが「コールバック地獄」と呼ばれるゆえんです。

---



### Kotlinの哲学：「軽量な並行処理を安全に、シンプルに」

Kotlinは「並行処理をもっと直感的に、安全に書けるようにしたい」という思想で、
**コルーチン（Coroutine）** を導入しました。

コルーチンは「スレッドの上で動く軽量なタスク」です。

* Threadより軽く、数万個でも動かせる
* `suspend` 関数によって中断と再開が可能
* スコープを使ってライフサイクルを安全に管理できる

---

### コルーチンとは？

コルーチンとは？

一言で言えば、

「軽量なスレッド＋構造化された非同期タスクの仕組み」

これを料理にたとえると、次のようになります。

Thread の場合（従来のやり方）

Threadは「一人のシェフが一皿ずつ最初から最後まで担当する」ようなものです。
カレーを作っている間は、そのシェフは他の仕事ができません。
オーブンで焼いている時間も、ただ突っ立って見ているしかない。

新しい料理を同時に作りたければ、もう一人シェフを雇う必要があります。
しかし人件費（＝リソース）も場所（＝メモリ）もどんどん増えます。

Coroutine の場合（Kotlin的アプローチ）

Coroutineは「少人数のキッチンスタッフが、必要なときだけ動いて手際よく料理を進める」ような仕組みです。

たとえば：

Aさんがスープを煮込んでいるあいだ（待ち時間）、
→ Bさんがサラダを盛りつける。

スープが煮えたらAさんがまた戻って仕上げをする。

ここでのポイントは、
「待っている時間を無駄にせず、他の作業に切り替えられる」 こと。
Kotlinのコルーチンはこの「切り替え」を自動で最適にやってくれます。

構造化並行性（チームプレーのルール）

さらにコルーチンでは、全員が「チーム単位」で動きます。

親シェフ（＝スコープ）がキッチンを閉めたら、
→ その下で動いているスタッフ（＝子コルーチン）も全員作業を止めて片付ける。

Threadだと「誰かが勝手に残業している」ような状態になりがちですが、
Coroutineではチーム全体が同じルールで動くため、後片付けやエラー処理が自動的に整理されるのです。


# コルーチンの基礎

## (1) Thread vs Coroutine

### Thread の特徴

* OS が直接管理する
* 生成・切り替えコストが高い
* 同時に大量のスレッドを動かすとメモリ負荷が大きい

```java
new Thread(() -> {
    Thread.sleep(1000);
    System.out.println("完了");
}).start();
```

このようなコードは少数なら問題ありませんが、数千単位で並行実行するとシステムが重くなります。

---

### Coroutine の特徴

* Kotlin ランタイムが管理（ユーザー空間で動作）
* 非常に軽量（数万単位で動かしてもOK）
* 同じスレッド上で複数の処理を切り替えて実行

```kotlin
GlobalScope.launch {
    delay(1000)
    println("完了")
}
```

`delay` の間に他の処理が動くため、スレッドを占有しません。

---

`比喩で理解する`

Thread = 人が対応するレジ

一人のお客さんを担当したら、会計が終わるまで他のお客さんを処理できない。

レジを増やすには店員（Thread）を増やす必要があり、人件費（メモリ）も場所（CPU）もかかる。

Coroutine = セルフレジ

1台の端末（スレッド）を複数の客が順番に使う。

1人が袋詰めしている間に、次の人が支払い画面に進める。

店員（OS）はレジ全体を見守るだけで、実際の進行は軽やかに切り替わる。

---

## (2) `suspend` 関数

### 基本の考え方

`suspend` は「一時停止できる関数」という意味です。
コルーチンの中で呼び出すと、処理を中断して別の作業に切り替えることができます。

```kotlin
suspend fun fetchData(): String {
    delay(1000)
    return "OK"
}
```

`delay(1000)` は `Thread.sleep(1000)` のようにスレッドを止めるわけではなく、
「この処理は1秒後に再開するから、その間は他の処理を進めてね」とKotlinに伝えます。

---

### イメージで捉える

`Thread.sleep()` は「部屋を借りて寝る」。
→ 他の人はその部屋（スレッド）を使えない。

`delay()` は「一度部屋を出て、1秒後に戻る」。
→ 他の人がその間に同じ部屋を使える。

これが「スレッドを止めずに処理を待つ」というコルーチンの強みです。

---

## (3) CoroutineScope と CoroutineContext

### CoroutineScope（スコープ）

スコープはコルーチンの「生存範囲」を決めるものです。
スコープが終わると、その中で動いているコルーチンも自動的にキャンセルされます。
これにより、メモリリークや孤立タスクを防げます。

```kotlin
coroutineScope {
    launch { println("A") }
    launch { println("B") }
} // ここを抜けると両方完了
```

---

### CoroutineContext（コンテキスト）

コンテキストはコルーチンの実行環境をまとめたものです。
中には以下のような情報が入っています。

* **Dispatcher**：どのスレッドで動くか（例：IO, Default, Main）
* **Job**：状態（実行中・キャンセル・完了）

例：

```kotlin
GlobalScope.launch(Dispatchers.IO) {
    println("実行スレッド: ${Thread.currentThread().name}")
}
```

上記では `Dispatchers.IO` によって、I/O 処理向けのスレッドプール上で実行されます。

---

### まとめ

* **Thread** は OS が管理する重い単位
* **Coroutine** は Kotlin が管理する軽い単位
* **suspend 関数** で処理を一時停止・再開できる
* **CoroutineScope/Context** により、安全で構造化された並行処理が実現できる

## 実践的な非同期処理

---

### (1) `launch` vs `async/await`

Kotlinのコルーチンでは、非同期処理を開始する方法がいくつかあります。
その代表が **`launch`** と **`async`** です。
どちらもコルーチンを起動しますが、「戻り値の有無」が大きな違いです。

---

#### `launch`：結果を返さない（fire-and-forget）

`launch` は「とにかく実行して終わり」という使い方をします。
結果を待たず、裏で非同期に動かします。

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    launch {
        delay(500)
        println("料理Aが完成！")
    }

    println("注文受付中…")
}
```

実行結果（例）：

```
注文受付中…
料理Aが完成！
```

このように、`launch` は“裏で動く軽作業”のようなイメージです。
完了を待つ必要がないログ送信やキャッシュ削除などに向いています。

---

#### `async`：結果を返す（`Deferred<T>`）

`async` は「非同期に処理をして結果を返す」ためのものです。
Javaでいう `Future` に似ています。

```kotlin
import kotlinx.coroutines.*

suspend fun fetchData(): String {
    delay(1000)
    return "データ取得完了"
}

fun main() = runBlocking {
    val deferred = async { fetchData() }  // 非同期で実行
    println("待機中…")
    println(deferred.await())             // 結果を待つ
}
```

出力：

```
待機中…
データ取得完了
```

* `async` は「並列で動かして、あとで結果をまとめる」ときに便利。
* `.await()` で結果を受け取るまで一時停止します。

---

#### 実践例：複数のAPIを同時に呼び出す

```kotlin
val a = async { fetchUserInfo() }
val b = async { fetchUserPosts() }
val c = async { fetchUserSettings() }

val user = a.await()
val posts = b.await()
val settings = c.await()
```

3つの処理を同時に動かし、結果が揃ったらまとめて使える。
Threadで書くよりも、ずっとシンプルです。

---

### (2) Flow API

`Flow` は **「非同期でデータを順番に流す仕組み」** です。
RxJavaのようなリアクティブストリームを、より軽量にKotlinで実現できます。

---

#### 基本形

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    flow {
        emit("A")
        delay(500)
        emit("B")
    }.collect { value ->
        println("受け取った: $value")
    }
}
```

出力：

```
受け取った: A
受け取った: B
```

* `emit()`：データを流す（送信する）
* `collect()`：データを受け取る（消費する）
* `delay()` を使ってもスレッドを止めないため、非同期に動作する

---

#### Flow の特徴

* **cold stream（コールドストリーム）**
  → `collect()` が呼ばれた瞬間に初めて実行が始まる。
* **キャンセルや例外にも対応**
  → 中断されたら自動的に安全に停止する。

比喩的に言えば、Flowは「注文が入ってから動くベルトコンベア」。
必要になるまで作業を始めないのでムダがありません。

---

#### 応用：データを段階的に処理

```kotlin
flow {
    (1..3).forEach {
        delay(300)
        emit(it)
    }
}.map { it * 2 }      // 各値を変換
 .filter { it > 2 }   // 条件で絞り込み
 .collect { println(it) }
```

出力：

```
4
6
```

シーケンス処理のように、データを1つずつリアルタイムで扱えます。

---

### (3) 例外処理とキャンセレーション

コルーチンの強みは「例外やキャンセルも安全に扱える」ことです。

---

#### try/catch で例外を補足

```kotlin
launch {
    try {
        error("エラー発生！")
    } catch (e: Exception) {
        println("例外をキャッチ: ${e.message}")
    }
}
```

---

#### coroutineScope：キャンセルの伝搬

`coroutineScope {}` を使うと、親子関係でキャンセルが伝わります。

```kotlin
suspend fun parentTask() = coroutineScope {
    launch { delay(1000); println("子A 完了") }
    launch { delay(2000); println("子B 完了") }
    throw RuntimeException("親がエラー！")
}
```

出力：

```
Exception in thread "main": 親がエラー！
```

→ 親が失敗すると、子のコルーチンも全てキャンセルされます。
これが **構造化並行性** の基本です。

---

#### withTimeout：タイムアウト制御

一定時間内に処理が終わらない場合、例外を発生させて中断できます。

```kotlin
try {
    withTimeout(1000) {
        delay(2000)
        println("完了")  // 実行されない
    }
} catch (e: TimeoutCancellationException) {
    println("タイムアウト発生！")
}
```

出力：

```
タイムアウト発生！
```

---

### まとめ

| 概念               | 役割           | 使いどころ             |
| ---------------- | ------------ | ----------------- |
| `launch`         | 結果を返さないコルーチン | 裏方処理（ログ送信、UI更新など） |
| `async/await`    | 結果を返すコルーチン   | 複数処理の並列実行         |
| `Flow`           | 非同期データストリーム  | 継続的なデータの受け取り      |
| `coroutineScope` | 親子関係の管理      | 構造化並行性の実現         |
| `withTimeout`    | タイムアウト制御     | 長時間処理の安全な中断       |

---

コルーチンの「非同期処理」は、複雑なスレッド操作を隠し、
**「必要なときに動き、不要になったら止まる」**
そんな自然で安全な並行処理の形を実現します。

## Java の並行処理との比較


### (1) CompletableFutureとの違い

Javaの `CompletableFuture` は、非同期処理を連続的につなげて書ける仕組みです。
ただし、使いこなそうとするとコードの見通しが悪くなりやすいです。

```java
CompletableFuture.supplyAsync(() -> fetchData())
    .thenApply(data -> process(data))
    .thenAccept(result -> System.out.println(result))
    .join();
```

この書き方では、

* `thenApply` や `join` などを明示的に組み合わせる必要がある
* 例外処理やキャンセル処理を自分で制御しないといけない
* スレッドがどこで動いているかを気にする必要がある

つまり、処理が複雑になると「どこで何が起きているか」を追うのが大変です。

---

Kotlinのコルーチンでは、非同期処理が自然な形で書けます。

```kotlin
suspend fun fetchData(): String {
    delay(1000)
    return "OK"
}

fun main() = runBlocking {
    val result = async { fetchData() }.await()
    println(result)
}
```

* `async` で非同期処理を開始
* `await()` で結果を待つ
* 見た目は同期処理のように直線的で読みやすい

さらに、Kotlinのコルーチンは「構造化並行性」を持っています。
これは、親子関係でタスクをまとめて管理できる仕組みです。

```kotlin
suspend fun parentTask() = coroutineScope {
    launch { taskA() }
    launch { taskB() }
}
```

親スコープが終われば、子のコルーチンも自動的に終了します。
これにより、非同期処理が放置されたり暴走したりすることを防げます。

---

#### 違いのまとめ

| 観点       | CompletableFuture (Java) | Coroutine (Kotlin) |
| -------- | ------------------------ | ------------------ |
| 書き方      | thenApplyやjoinを組み合わせる    | await()で直感的に書ける    |
| タスク管理    | 個別に制御                    | スコープで自動管理          |
| 例外・キャンセル | 自分で制御が必要                 | 親子関係で自動伝播          |
| コードの見通し  | 手続きが増えるほど複雑化             | シンプルで読みやすい         |

---

### (2) スレッドプールとディスパッチャー

Javaでは `ExecutorService` などを使ってスレッドプールを自分で管理します。
「どのスレッドで動かすか」を設計者が決める必要があります。

Kotlinでは、`Dispatchers`（ディスパッチャー）がスレッドの種類を自動的に割り振ります。
開発者は「どんなタイプの処理か」だけを指定します。

---

#### 主なディスパッチャー

| Dispatcher            | 主な用途     | 説明                             |
| --------------------- | -------- | ------------------------------ |
| `Dispatchers.Default` | CPUを使う処理 | 並列計算・データ加工などCPU負荷の高い処理に最適      |
| `Dispatchers.IO`      | I/O処理    | ファイル・DB・API通信など待ち時間の長い処理に適している |
| `Dispatchers.Main`    | UI処理     | AndroidなどのUIスレッド上で動かすために使う     |

---

#### 例

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    launch(Dispatchers.Default) {
        println("CPU系処理: ${Thread.currentThread().name}")
    }

    launch(Dispatchers.IO) {
        println("I/O系処理: ${Thread.currentThread().name}")
    }
}
```

出力例：

```
CPU系処理: DefaultDispatcher-worker-1
I/O系処理: DefaultDispatcher-worker-2
```

スレッドを意識せず、処理の種類に応じて最適な場所で動かせます。

---

#### 違いのまとめ

| 観点        | Java                   | Kotlin           |
| --------- | ---------------------- | ---------------- |
| スレッド管理    | ExecutorServiceなどを手動設定 | Dispatchersが自動制御 |
| 並行処理の単位   | Thread / Future        | Coroutine        |
| コードの書きやすさ | 明示的で複雑                 | シンプルで意図が明確       |
| 安全性       | 制御を間違えるとリソース浪費         | スコープで自動管理される     |

---

Kotlinのコルーチンは、Javaの非同期処理をより自然で安全に扱えるようにした仕組みです。
開発者はスレッドやタスクの細かい制御ではなく、**「何を非同期で実行したいか」** に集中できます。

## 構造化並行性の概念

### コルーチンがもたらす最大の革命

Kotlinのコルーチンが本質的に優れている理由は、
単に“軽量なスレッド”だからではありません。
最大の価値は、**「構造化並行性（Structured Concurrency）」** という仕組みにあります。

これは、**「非同期処理をスコープ（範囲）という枠の中で安全に管理する」**という発想です。

---

### なぜ構造化が必要なのか

従来のスレッドベースの並行処理では、
スレッドを作った人と終了させる人が別になることがよくあります。

```java
new Thread(() -> {
    doSomething();
}).start();

// この後、親スレッドは終了しても子スレッドは動き続ける
```

このように「親が終わっても子が生き続ける」構造では、

* メモリリーク
* 処理の競合
* 終了タイミングの曖昧さ
  といった問題が起こりやすく、
  実際の現場では**“非同期の後片付け”が最もバグを生む部分**になっていました。

---

### コルーチンの考え方

コルーチンでは、すべての非同期処理を**スコープ**の中で動かします。
スコープを抜ける（＝ブロックを出る）と、その中で動いている子コルーチンはすべて完了している状態になります。

つまり、

* 「親が生きている限り子も生きる」
* 「親がキャンセルされたら、子も全員止まる」
  という明確なルールで管理されます。

---

### 実際の例

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    coroutineScope {
        launch {
            delay(1000)
            println("taskA 完了")
        }
        launch {
            delay(500)
            println("taskB 完了")
        }
    }
    println("すべてのタスク完了")
}
```

出力：

```
taskB 完了
taskA 完了
すべてのタスク完了
```

`coroutineScope` ブロックを抜けた時点で、
すべての `launch` が確実に終わっています。
この「完了保証」は、従来のスレッドにはありませんでした。

---

### スコープがもたらす3つの安全性

1. **ライフサイクルの一貫性**
   親スコープを抜けた時点で、子の作業はすべて終了。
   → 「いつ終わるのか」が明確。

2. **キャンセル伝播**
   親が止まれば、子も自動的に止まる。
   → 途中キャンセル時の中途半端な状態がなくなる。

3. **例外処理の一元化**
   子コルーチンのエラーが親に伝わる。
   → 例外をスコープ単位で扱えるため、エラー処理が整理される。

---

### もう少し深い話：`coroutineScope` と `supervisorScope`

Kotlinには、スコープに似た2つの構造があります。

* `coroutineScope`
  → 子のどれかが失敗すると、他の子もすべてキャンセルされる（安全重視）
* `supervisorScope`
  → 一つの失敗で全体を止めない（独立タスク重視）

```kotlin
supervisorScope {
    launch { taskA() } // 失敗しても他のlaunchは続く
    launch { taskB() }
}
```

状況に応じてどちらのスコープを使うか選べるのが、Kotlinの柔軟さです。

---

### Threadとの対比

| 項目     | Thread    | Coroutine        |
| ------ | --------- | ---------------- |
| 管理単位   | 個々のスレッド   | スコープ（まとまり）       |
| 親子関係   | なし        | 明確に存在する          |
| 終了の扱い  | 明示的に制御が必要 | スコープを抜けると自動完了    |
| 例外伝播   | 親に伝わらない   | 親に自動で伝わる         |
| メモリリーク | 起きやすい     | 起きにくい（自動クリーンアップ） |

---

### 構造化並行性の利点

* **コードの流れがわかりやすい**：非同期でも“順番に読める”
* **メンテナンス性が高い**：スコープごとに責任範囲を分離できる
* **バグが減る**：放置タスク・ゾンビ処理が発生しない

構造化並行性は、単に「非同期を使いやすくする」ものではなく、
**非同期を“安全に制御可能な構造”に変える設計思想**です。

---

### まとめ

コルーチンの構造化並行性は、

> 「非同期処理を1つのまとまりとして安全に管理する」
> という考え方をプログラムの構文として保証するものです。

スレッドのように“作って放置する”のではなく、
“スコープを抜ける＝すべての処理が終わる”という直感的で安全な構造。

この設計思想が、コルーチンが「軽いだけでなく、安心して使える非同期処理」を実現している最大の理由です。


## まとめ

### コルーチンの要点

Kotlinのコルーチンは、**「複雑な非同期処理をシンプルに、安全に扱う」**ための仕組みです。
ここまで学んできた内容を、もう一度整理しておきましょう。

---

#### 1. 軽量で安全な非同期処理

コルーチンはスレッドよりもずっと軽く、数万単位で同時に動かせます。
しかもスコープ（範囲）で管理されるため、処理の漏れや暴走を防げます。

---

#### 2. `suspend` で中断可能

`suspend` 関数を使うことで、処理を一時停止し、
再開したいタイミングで自然に続きが実行されます。
スレッドを止めるのではなく、**「処理の流れを一時的に預ける」**イメージです。

---

#### 3. `async` / `await` で自然な並列処理

`async` で並行実行、`await()` で結果を待つだけ。
複数の処理を同時に動かして、結果をまとめるコードがとても書きやすくなります。

---

#### 4. `Flow` で非同期ストリーム処理

`Flow` はデータを「流れ」として非同期に扱える仕組みです。
APIの定期ポーリングやリアルタイム更新など、
「少しずつデータが届く」ようなケースに最適です。

---

#### 5. 構造化並行性で安全なキャンセル

すべての非同期処理はスコープで管理され、
親が終われば子も確実に終わります。
エラーやキャンセルも自動で伝わるため、**「後始末まで含めて安全」**です。

---

### コルーチンがもたらす新しい並行処理モデル

これまでのJavaの並行処理は、

* スレッドを明示的に作って動かす
* FutureやExecutorを手動で管理する
  という“道具を操る”時代でした。

一方で、Kotlinのコルーチンは、

> 「何をしたいか」を書くだけで、
> 「どう並行して動くか」は仕組みが自動で最適化してくれる。

コードの見通しがよく、スレッドの複雑さを意識せずに並行処理が書けます。

---

### 最後に

> Javaの「Future」は過去の遺産、
> Kotlinの「Coroutine」は未来の並行処理モデル。

コルーチンは単なる“非同期ライブラリ”ではなく、
**「プログラムの時間の流れをデザインするための構文」**です。

これを理解すれば、
複雑だった「並行処理」が、まるで直線的なストーリーのように書けるようになります。
そしてそれが、Kotlinが目指す「読みやすく、安全で、強力なコード」の核心です。

### 確認問題①：`launch` と `async` の違い（実践編）

次のコードを見てください。
２つのAPIを同時に呼び出し、結果をまとめて出力したいとします。
このコードは意図通りに動作するでしょうか？

```kotlin
import kotlinx.coroutines.*

suspend fun fetchUser(): String {
    delay(1000)
    return "ユーザー情報"
}

suspend fun fetchPosts(): String {
    delay(1000)
    return "投稿一覧"
}

fun main() = runBlocking {
    val user = launch { fetchUser() }
    val posts = launch { fetchPosts() }

    println("結果: ${user} ＋ ${posts}")
}
```

**質問：**
このコードがうまく動かない理由と、正しい修正方法を答えてください。

---

> ✅ **解答例：**
> このコードは `launch` を使っているため、結果を返していません。
> `launch` は「結果を返さない非同期処理（fire-and-forget）」なので、`fetchUser()` や `fetchPosts()` の戻り値を受け取れません。
>
> 結果をまとめたい場合は、`async` を使って `Deferred` を受け取り、`await()` で結果を待ちます。
>
> ```kotlin
> fun main() = runBlocking {
>     val user = async { fetchUser() }
>     val posts = async { fetchPosts() }
>
>     println("結果: ${user.await()} ＋ ${posts.await()}")
> }
> ```
>
> `async` は非同期に結果を返せるため、2つの処理を同時に走らせつつ、結果を安全に取得できます。

---

### 確認問題②：構造化並行性の理解（実践編）

次のコードでは、２つのタスクを同時に実行しています。
親スコープでキャンセルを発生させた場合、どうなるでしょうか？

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val job = launch {
        coroutineScope {
            launch {
                repeat(5) {
                    delay(500)
                    println("タスクA: 実行中 ($it)")
                }
            }

            launch {
                repeat(5) {
                    delay(500)
                    println("タスクB: 実行中 ($it)")
                }
            }
        }
    }

    delay(1000)
    println("親スコープをキャンセル！")
    job.cancelAndJoin()
    println("すべての処理が停止しました")
}
```

**質問：**
このときの実行結果はどうなりますか？
また、`coroutineScope` を使わずに書いた場合、どんな問題が起こり得るでしょうか？

---

> ✅ **解答例：**
> 出力はおおよそ次のようになります。
>
> ```
> タスクA: 実行中 (0)
> タスクB: 実行中 (0)
> タスクA: 実行中 (1)
> タスクB: 実行中 (1)
> 親スコープをキャンセル！
> すべての処理が停止しました
> ```
>
> 親スコープをキャンセルした時点で、スコープ内の全ての子コルーチン (`タスクA`・`タスクB`) がまとめて停止します。
> これが構造化並行性のポイントであり、**キャンセルや終了を親子単位で安全に伝播できる**ようになっています。
>
> もし `coroutineScope` を使わずに個別の `GlobalScope.launch` などで動かした場合、
> 親がキャンセルされても子が動き続け、ログが出続けることになります。
> その結果、**ゾンビタスク（放置された非同期処理）** が発生し、リソースリークや整合性の問題を引き起こす危険があります。
