# 第7章：コルーチン入門 - 非同期処理の新しいアプローチ

## 7.1 コルーチンの基礎

### ThreadとCoroutineの違い

コルーチンは、軽量スレッドとも呼ばれ、従来のスレッドと比較して以下の特徴があります。

```kotlin
// スレッドを使った並行処理
fun withThreads() {
    repeat(100_000) {
        thread {
            Thread.sleep(1000)
            print(".")
        }
    }
    // メモリ不足になる可能性が高い
}

// コルーチンを使った並行処理
import kotlinx.coroutines.*

fun withCoroutines() = runBlocking {
    repeat(100_000) {
        launch {
            delay(1000)
            print(".")
        }
    }
    // メモリ効率が良く、問題なく実行できる
}
```

**主な違い：**

| 特徴 | Thread | Coroutine |
|------|--------|-----------|
| コスト | 重い（1MB程度） | 軽い（数KB） |
| 作成数 | 制限あり（数千） | 実質無制限（数十万） |
| コンテキストスイッチ | OSレベル（遅い） | ユーザーレベル（速い） |
| 制御 | 複雑 | 構造化された並行性 |

**Javaとの比較：**

```java
// Java - Thread
new Thread(() -> {
    try {
        Thread.sleep(1000);
        System.out.println("Done");
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}).start();

// Java - CompletableFuture
CompletableFuture.supplyAsync(() -> {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
    return "Done";
}).thenAccept(System.out::println);

// Java - Virtual Threads (Java 21+)
Thread.startVirtualThread(() -> {
    try {
        Thread.sleep(1000);
        System.out.println("Done");
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
});
```

### Suspend関数

`suspend`修飾子を付けた関数は、コルーチン内で実行を一時停止できます。

```kotlin
// suspend関数の定義
suspend fun fetchUserData(userId: Long): User {
    delay(1000)  // コルーチンを1秒間一時停止
    return User(userId, "User $userId")
}

suspend fun fetchUserPosts(userId: Long): List<Post> {
    delay(500)
    return listOf(Post(1, "Post 1"), Post(2, "Post 2"))
}

// suspend関数は他のsuspend関数またはコルーチン内でのみ呼び出せる
suspend fun getUserProfile(userId: Long): UserProfile {
    val user = fetchUserData(userId)  // OK
    val posts = fetchUserPosts(userId)  // OK
    return UserProfile(user, posts)
}

// 通常の関数からは呼び出せない
fun normalFunction() {
    // fetchUserData(1)  // コンパイルエラー！
}

// コルーチンビルダーを使って呼び出す
fun callSuspendFunction() = runBlocking {
    val user = fetchUserData(1)  // OK
    println(user)
}

// より実践的な例
suspend fun loginUser(email: String, password: String): Result<User> {
    return try {
        // ネットワークリクエストをシミュレート
        delay(1000)

        // 認証処理
        val token = authenticateUser(email, password)
        val user = fetchUserByToken(token)

        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun authenticateUser(email: String, password: String): String {
    delay(500)
    return "auth-token-123"
}

suspend fun fetchUserByToken(token: String): User {
    delay(500)
    return User(1, "Authenticated User")
}
```

### CoroutineScopeとCoroutineContext

コルーチンスコープは、コルーチンのライフサイクルを管理します。

```kotlin
import kotlinx.coroutines.*

// runBlocking - メインスレッドをブロック（主にテストで使用）
fun main() = runBlocking {
    println("Start")
    delay(1000)
    println("End")
}

// GlobalScope - アプリケーション全体のスコープ（通常は避けるべき）
fun badExample() {
    GlobalScope.launch {
        delay(1000)
        println("This might not execute if app exits early")
    }
    // アプリが終了すると、コルーチンも終了してしまう
}

// カスタムスコープの作成
class MyRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun fetchData() {
        scope.launch {
            val data = performNetworkRequest()
            processData(data)
        }
    }

    fun cleanup() {
        scope.cancel()  // すべてのコルーチンをキャンセル
    }

    private suspend fun performNetworkRequest(): String {
        delay(1000)
        return "Data"
    }

    private fun processData(data: String) {
        println("Processing: $data")
    }
}

// CoroutineContext - コルーチンの実行コンテキスト
fun contextExample() = runBlocking {
    println("Main: ${Thread.currentThread().name}")

    // コンテキストの要素
    launch(Dispatchers.Default) {
        println("Default: ${Thread.currentThread().name}")
    }

    launch(Dispatchers.IO) {
        println("IO: ${Thread.currentThread().name}")
    }

    launch(Dispatchers.Main) {
        // Android/JavaFXのメインスレッド
        println("Main: ${Thread.currentThread().name}")
    }

    launch(Dispatchers.Unconfined) {
        println("Unconfined: ${Thread.currentThread().name}")
    }
}

// 構造化された並行性
fun structuredConcurrency() = runBlocking {
    println("Start")

    launch {
        delay(1000)
        println("Task 1")
    }

    launch {
        delay(500)
        println("Task 2")
    }

    println("End")  // すべての子コルーチンが完了するまで待つ
}

// 出力:
// Start
// End
// Task 2 (500ms後)
// Task 1 (1000ms後)
```

### コルーチンビルダー

```kotlin
import kotlinx.coroutines.*

// launch - 結果を返さないコルーチンを起動
fun launchExample() = runBlocking {
    val job = launch {
        delay(1000)
        println("Launch completed")
    }

    println("Launch started")
    job.join()  // コルーチンの完了を待つ
    println("Launch finished")
}

// async - 結果を返すコルーチンを起動
fun asyncExample() = runBlocking {
    val deferred = async {
        delay(1000)
        "Result"
    }

    println("Async started")
    val result = deferred.await()  // 結果を待つ
    println("Result: $result")
}

// 複数の非同期処理を並列実行
suspend fun fetchMultipleData() = coroutineScope {
    val user = async { fetchUserData(1) }
    val posts = async { fetchUserPosts(1) }
    val comments = async { fetchUserComments(1) }

    // すべての結果を待つ
    UserDashboard(
        user = user.await(),
        posts = posts.await(),
        comments = comments.await()
    )
}

// withContext - コンテキストを切り替えて実行
suspend fun loadData(): String = withContext(Dispatchers.IO) {
    // IOスレッドで実行
    performNetworkRequest()
}

// coroutineScope - 構造化されたスコープを作成
suspend fun processData() = coroutineScope {
    launch {
        delay(500)
        println("Task 1")
    }

    launch {
        delay(300)
        println("Task 2")
    }

    // すべての子が完了するまで待つ
}

// supervisorScope - 子の失敗を隔離
suspend fun robustProcessing() = supervisorScope {
    val job1 = launch {
        delay(100)
        throw Exception("Job 1 failed")
    }

    val job2 = launch {
        delay(200)
        println("Job 2 succeeded")
    }

    // job1が失敗してもjob2は継続
}
```

## 7.2 実践的な非同期処理

### async/await

```kotlin
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

// 逐次実行（遅い）
suspend fun sequentialExecution() {
    val time = measureTimeMillis {
        val user = fetchUserData(1)  // 1秒
        val posts = fetchUserPosts(1)  // 0.5秒
        println("User: $user, Posts: $posts")
    }
    println("Sequential time: $time ms")  // 約1500ms
}

// 並列実行（速い）
suspend fun parallelExecution() = coroutineScope {
    val time = measureTimeMillis {
        val userDeferred = async { fetchUserData(1) }  // 1秒
        val postsDeferred = async { fetchUserPosts(1) }  // 0.5秒

        val user = userDeferred.await()
        val posts = postsDeferred.await()
        println("User: $user, Posts: $posts")
    }
    println("Parallel time: $time ms")  // 約1000ms
}

// 実践的な例：複数APIの並列呼び出し
data class Dashboard(
    val user: User,
    val posts: List<Post>,
    val notifications: List<Notification>,
    val stats: Statistics
)

suspend fun loadDashboard(userId: Long): Dashboard = coroutineScope {
    // すべてのAPIを並列で呼び出す
    val userDeferred = async { fetchUserData(userId) }
    val postsDeferred = async { fetchUserPosts(userId) }
    val notificationsDeferred = async { fetchNotifications(userId) }
    val statsDeferred = async { fetchStatistics(userId) }

    // すべての結果を待つ
    Dashboard(
        user = userDeferred.await(),
        posts = postsDeferred.await(),
        notifications = notificationsDeferred.await(),
        stats = statsDeferred.await()
    )
}

// エラーハンドリング付き
suspend fun loadDashboardSafe(userId: Long): Result<Dashboard> = runCatching {
    coroutineScope {
        val userDeferred = async { fetchUserData(userId) }
        val postsDeferred = async { fetchUserPosts(userId) }
        val notificationsDeferred = async { fetchNotifications(userId) }
        val statsDeferred = async { fetchStatistics(userId) }

        Dashboard(
            user = userDeferred.await(),
            posts = postsDeferred.await(),
            notifications = notificationsDeferred.await(),
            stats = statsDeferred.await()
        )
    }
}

// 一部の失敗を許容する
suspend fun loadDashboardResilient(userId: Long): Dashboard = coroutineScope {
    val userDeferred = async { fetchUserData(userId) }
    val postsDeferred = async {
        try {
            fetchUserPosts(userId)
        } catch (e: Exception) {
            emptyList()  // 失敗時は空リスト
        }
    }
    val notificationsDeferred = async {
        try {
            fetchNotifications(userId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    Dashboard(
        user = userDeferred.await(),
        posts = postsDeferred.await(),
        notifications = notificationsDeferred.await(),
        stats = Statistics.default()
    )
}
```

**Javaとの比較：**

```java
// Java - CompletableFuture
CompletableFuture<User> userFuture =
    CompletableFuture.supplyAsync(() -> fetchUserData(1));
CompletableFuture<List<Post>> postsFuture =
    CompletableFuture.supplyAsync(() -> fetchUserPosts(1));

CompletableFuture<Dashboard> dashboardFuture = userFuture
    .thenCombine(postsFuture, (user, posts) ->
        new Dashboard(user, posts)
    );

Dashboard dashboard = dashboardFuture.join();
```

### Flow API

Flowは、複数の値を非同期に発行するストリームです。

```kotlin
import kotlinx.coroutines.flow.*

// Flowの基本
fun simpleFlow(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)
        emit(i)  // 値を発行
    }
}

// Flowの収集
fun collectFlow() = runBlocking {
    simpleFlow().collect { value ->
        println(value)
    }
}

// Flowビルダー
fun flowBuilders() = runBlocking {
    // flowOf - 固定値のFlow
    flowOf(1, 2, 3, 4, 5)
        .collect { println(it) }

    // asFlow - コレクションからFlow
    listOf(1, 2, 3, 4, 5)
        .asFlow()
        .collect { println(it) }

    // channelFlow - チャンネルベースのFlow
    channelFlow {
        for (i in 1..3) {
            send(i)
        }
    }.collect { println(it) }
}

// Flowの変換
fun flowTransformations() = runBlocking {
    // map
    flowOf(1, 2, 3)
        .map { it * 2 }
        .collect { println(it) }  // 2, 4, 6

    // filter
    flowOf(1, 2, 3, 4, 5)
        .filter { it % 2 == 0 }
        .collect { println(it) }  // 2, 4

    // transform - 複雑な変換
    flowOf(1, 2, 3)
        .transform { value ->
            emit("Emitting $value")
            emit(value * 2)
        }
        .collect { println(it) }
}

// 実践的な例：リアルタイムデータストリーム
fun userActivityStream(userId: Long): Flow<UserActivity> = flow {
    while (true) {
        delay(1000)  // 1秒ごと
        val activity = fetchLatestActivity(userId)
        emit(activity)
    }
}

// Flowの合成
suspend fun monitorUser(userId: Long) {
    userActivityStream(userId)
        .filter { it.isImportant }
        .map { it.message }
        .collect { message ->
            println("Important: $message")
        }
}

// 複数のFlowの結合
fun combinedStream(): Flow<String> = flow {
    val flow1 = flowOf("A", "B", "C")
    val flow2 = flowOf(1, 2, 3)

    flow1.zip(flow2) { letter, number ->
        "$letter$number"
    }.collect { emit(it) }
}

// エラーハンドリング
fun flowWithErrorHandling() = runBlocking {
    flow {
        emit(1)
        emit(2)
        throw RuntimeException("Error!")
        emit(3)
    }.catch { e ->
        println("Caught: ${e.message}")
        emit(-1)  // エラー時のデフォルト値
    }.collect { println(it) }
}

// バックプレッシャー処理
fun flowWithBackpressure() = runBlocking {
    flow {
        repeat(10) {
            emit(it)
            println("Emitted $it")
        }
    }
    .buffer()  // バッファリング
    .collect { value ->
        delay(100)  // 遅い処理
        println("Collected $value")
    }
}

// StateFlow - 状態を持つFlow
class ViewModel {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            try {
                val data = fetchData()
                _uiState.value = UiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class UiState {
    object Loading : UiState()
    data class Success(val data: String) : UiState()
    data class Error(val message: String) : UiState()
}

// SharedFlow - 複数のコレクターで共有
class EventBus {
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    suspend fun postEvent(event: Event) {
        _events.emit(event)
    }
}
```

### 例外処理とキャンセレーション

```kotlin
import kotlinx.coroutines.*

// 例外処理
fun exceptionHandling() = runBlocking {
    // try-catch
    try {
        coroutineScope {
            launch {
                delay(100)
                throw Exception("Failed!")
            }
        }
    } catch (e: Exception) {
        println("Caught: ${e.message}")
    }

    // CoroutineExceptionHandler
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught: ${exception.message}")
    }

    val scope = CoroutineScope(Dispatchers.Default + handler)
    scope.launch {
        throw Exception("Failed!")
    }

    delay(100)
}

// キャンセレーション
fun cancellationExample() = runBlocking {
    val job = launch {
        repeat(1000) { i ->
            println("Job: $i")
            delay(500)
        }
    }

    delay(1300)  // 少し待つ
    println("Cancelling job...")
    job.cancel()  // キャンセル
    job.join()  // 完了を待つ
    println("Job cancelled")
}

// キャンセル可能なコード
suspend fun cancellableWork() {
    repeat(1000) { i ->
        // isActiveでキャンセルをチェック
        if (!isActive) {
            println("Work cancelled")
            return
        }

        // または ensureActive()
        ensureActive()

        println("Working: $i")
        delay(100)
    }
}

// リソースのクリーンアップ
fun resourceCleanup() = runBlocking {
    val job = launch {
        try {
            repeat(1000) { i ->
                println("Working: $i")
                delay(100)
            }
        } finally {
            // キャンセル時のクリーンアップ
            println("Cleaning up...")
            withContext(NonCancellable) {
                // キャンセル不可能なクリーンアップ処理
                delay(100)
                println("Cleanup done")
            }
        }
    }

    delay(300)
    job.cancelAndJoin()
}

// タイムアウト
fun timeoutExample() = runBlocking {
    try {
        withTimeout(1000) {
            repeat(1000) { i ->
                println("Working: $i")
                delay(500)
            }
        }
    } catch (e: TimeoutCancellationException) {
        println("Timed out!")
    }

    // nullを返すバージョン
    val result = withTimeoutOrNull(1000) {
        delay(1500)
        "Result"
    }
    println(result)  // null
}

// 実践的な例：リトライロジック
suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            println("Failed attempt ${it + 1}, retrying in ${currentDelay}ms")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return block()  // 最後の試行
}

// 使用例
suspend fun fetchDataWithRetry(): String {
    return retryWithBackoff(times = 3) {
        fetchData()
    }
}
```

## 7.3 Javaの並行処理との比較

### CompletableFutureとの違い

```kotlin
// Java CompletableFuture
CompletableFuture<User> userFuture = CompletableFuture
    .supplyAsync(() -> fetchUser(1))
    .thenApply(user -> user.toUpperCase())
    .exceptionally(ex -> {
        ex.printStackTrace();
        return null;
    });

// Kotlin Coroutines
suspend fun fetchUserAsync(id: Long): User {
    return try {
        val user = fetchUser(id)
        user.copy(name = user.name.uppercase())
    } catch (e: Exception) {
        e.printStackTrace()
        User.default()
    }
}

// 複数の非同期処理の組み合わせ
// Java
CompletableFuture<String> combined = CompletableFuture
    .supplyAsync(() -> fetchUser(1))
    .thenCombine(
        CompletableFuture.supplyAsync(() -> fetchPosts(1)),
        (user, posts) -> user.getName() + ": " + posts.size()
    );

// Kotlin
suspend fun getCombinedData(userId: Long): String = coroutineScope {
    val user = async { fetchUser(userId) }
    val posts = async { fetchPosts(userId) }
    "${user.await().name}: ${posts.await().size}"
}
```

### スレッドプールとディスパッチャー

```kotlin
// Javaのスレッドプール
ExecutorService executor = Executors.newFixedThreadPool(4);
executor.submit(() -> {
    // 処理
});
executor.shutdown();

// Kotlinのディスパッチャー
// Default - CPU集約的な処理用
launch(Dispatchers.Default) {
    val result = performHeavyComputation()
}

// IO - I/O処理用（ファイル、ネットワーク等）
launch(Dispatchers.IO) {
    val data = readFile()
}

// Main - UIスレッド（Android/JavaFX）
launch(Dispatchers.Main) {
    updateUI(data)
}

// カスタムディスパッチャー
val customDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
launch(customDispatcher) {
    // カスタムスレッドプールで実行
}
customDispatcher.close()  // 使用後はクローズ

// ディスパッチャーの切り替え
suspend fun loadAndDisplay() {
    val data = withContext(Dispatchers.IO) {
        // IOスレッドでデータ取得
        loadData()
    }

    withContext(Dispatchers.Main) {
        // メインスレッドでUI更新
        displayData(data)
    }
}
```

### 構造化並行性の概念

```kotlin
// 構造化並行性 - 親子関係の管理
fun structuredConcurrency() = runBlocking {
    println("Parent started")

    launch {
        delay(1000)
        println("Child 1 finished")
    }

    launch {
        delay(500)
        println("Child 2 finished")
    }

    println("Parent waiting for children...")
    // 自動的にすべての子コルーチンの完了を待つ
}

// 子のキャンセルは親に伝播
fun cancellationPropagation() = runBlocking {
    val parent = launch {
        val child1 = launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("Child 1 cancelled")
            }
        }

        val child2 = launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("Child 2 cancelled")
            }
        }
    }

    delay(100)
    parent.cancel()  // 親をキャンセルすると子も自動的にキャンセル
}

// SupervisorJob - 子の失敗を隔離
fun supervisorExample() = runBlocking {
    val supervisor = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.Default + supervisor)

    val child1 = scope.launch {
        delay(100)
        throw Exception("Child 1 failed")
    }

    val child2 = scope.launch {
        delay(200)
        println("Child 2 succeeded")
    }

    delay(300)
    // child1が失敗してもchild2は実行される
}
```

## まとめ

本章では、Kotlinコルーチンの基礎について学びました：

✅ **軽量で効率的な並行処理の仕組み**
✅ **suspend関数による一時停止可能な処理**
✅ **async/awaitによる並列実行**
✅ **Flowによる非同期データストリーム**
✅ **構造化並行性による安全なコルーチン管理**

Kotlinコルーチンにより、従来のスレッドやCompletableFutureよりも簡潔で読みやすい非同期コードを書くことができます。

## 演習問題

1. 3つのAPIエンドポイントから並列でデータを取得し、すべての結果を結合して返す関数を実装してください。1つでも失敗した場合はエラーを返すようにしてください。

2. リトライロジックを持つsuspend関数を実装してください。最大3回まで再試行し、各試行の間に指数バックオフ（100ms, 200ms, 400ms）を入れます。

3. Flowを使って、1秒ごとに現在時刻を発行し続けるストリームを実装してください。収集側では、時刻をフォーマットして表示します。