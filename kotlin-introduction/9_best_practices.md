# 第9章：ベストプラクティスとイディオム

## 9.1 Kotlinらしいコードの書き方

### 命名規則とコーディング規約

Kotlinの公式コーディング規約に従うことで、読みやすく保守しやすいコードになります。

```kotlin
// クラス名：パスカルケース
class UserManager
class OrderProcessor
class DatabaseConnection

// 関数・変数名：キャメルケース
fun calculateTotal()
val userName = "Alice"
var itemCount = 0

// 定数：大文字スネークケース
const val MAX_RETRY_COUNT = 3
const val DEFAULT_TIMEOUT_MS = 5000

object Constants {
    const val API_BASE_URL = "https://api.example.com"
    const val DEFAULT_PAGE_SIZE = 20
}

// プロパティ：キャメルケース
class User {
    val firstName: String = ""
    var emailAddress: String = ""
    private val internalId: Long = 0
}

// バッキングプロパティ：アンダースコアプレフィックス
class ViewModel {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}

// 拡張関数：レシーバ型に意味のある名前を
fun String.isValidEmail(): Boolean = contains("@")
fun List<Int>.average(): Double = sum().toDouble() / size

// ジェネリック型パラメータ
// T: Type
// E: Element
// K: Key
// V: Value
// R: Return type
class Repository<T>
class Cache<K, V>
fun <T, R> List<T>.mapNotNull(transform: (T) -> R?): List<R>

// ファイル名：クラス名と一致
// UserManager.kt -> class UserManager
// StringExtensions.kt -> String拡張関数群

// パッケージ名：小文字のみ、ドット区切り
package com.example.myapp
package com.example.myapp.domain.model
package com.example.myapp.data.repository
```

### よく使われるイディオム集

```kotlin
// 1. スコープ関数の適切な使用

// apply - オブジェクトの初期化
val user = User().apply {
    name = "Alice"
    email = "alice@example.com"
    age = 25
}

// also - デバッグ、ロギング
val result = processData(input)
    .also { println("Result: $it") }
    .also { log.debug("Processing completed") }

// let - nullチェックと変換
val length = nullableString?.let {
    println("String: $it")
    it.length
} ?: 0

// run - オブジェクトの設定と計算
val result = configuration.run {
    connect()
    authenticate()
    fetchData()
}

// with - 複数操作の集約（レシーバなし）
val message = with(StringBuilder()) {
    append("Hello")
    append(" ")
    append("World")
    toString()
}

// 2. 条件付き初期化

// takeIf / takeUnless
val validInput = input.takeIf { it.isNotBlank() }
val nonZeroValue = value.takeUnless { it == 0 }

// 実践例
fun processUser(user: User?) {
    user?.takeIf { it.isActive }
        ?.let { activeUser ->
            sendNotification(activeUser)
        }
}

// 3. コレクション操作の連鎖

// 読みやすい連鎖
val result = users
    .filter { it.age >= 18 }
    .map { it.name }
    .sorted()
    .take(10)

// 複雑な条件
val activeAdultUsers = users
    .filter { it.isActive }
    .filter { it.age >= 18 }
    .sortedByDescending { it.registrationDate }

// 4. 安全なキャストとnullチェック

// as? と ?: の組み合わせ
val string = value as? String ?: return
val length = (value as? String)?.length ?: 0

// when での型チェック
fun describe(obj: Any) = when (obj) {
    is String -> "String: $obj"
    is Int -> "Number: $obj"
    is List<*> -> "List of ${obj.size} items"
    else -> "Unknown"
}

// 5. 文字列テンプレート

// 変数の埋め込み
val greeting = "Hello, $name!"

// 式の埋め込み
val info = "Age in 10 years: ${age + 10}"

// プロパティアクセス
val message = "User: ${user.name}, Email: ${user.email}"

// 複雑な式
val summary = "Total: ${items.sumOf { it.price }}"

// 6. デフォルト引数と名前付き引数

// デフォルト引数で柔軟な関数定義
fun createConnection(
    host: String = "localhost",
    port: Int = 8080,
    timeout: Int = 5000,
    secure: Boolean = false
) {
    // ...
}

// 名前付き引数で可読性向上
val connection = createConnection(
    host = "api.example.com",
    port = 443,
    secure = true
)

// 7. データクラスとcopy

// イミュータブルな更新
val user = User(id = 1, name = "Alice", email = "alice@example.com")
val updatedUser = user.copy(email = "newemail@example.com")

// 8. シングルトン（object宣言）

object AppConfig {
    const val VERSION = "1.0.0"
    val startTime = System.currentTimeMillis()

    fun initialize() {
        // 初期化処理
    }
}

// 9. sealed class for状態管理

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

fun <T> handleResult(result: Result<T>) {
    when (result) {
        is Result.Success -> println("Data: ${result.data}")
        is Result.Error -> println("Error: ${result.message}")
        Result.Loading -> println("Loading...")
    }
}

// 10. require/check/assert

fun processOrder(order: Order) {
    // 前提条件のチェック
    require(order.items.isNotEmpty()) { "Order must have items" }
    require(order.total > 0) { "Order total must be positive" }

    // 状態のチェック
    check(order.status == OrderStatus.PENDING) {
        "Order must be in PENDING status"
    }

    // 開発時のアサーション
    assert(order.id > 0) { "Order ID must be positive" }
}
```

### アンチパターンと回避方法

```kotlin
// アンチパターン 1: 不要な!! の使用

// 悪い例
fun badExample(user: User?) {
    val name = user!!.name
    val email = user!!.email  // 2回もチェック
}

// 良い例
fun goodExample(user: User?) {
    val user = user ?: return  // early return
    val name = user.name
    val email = user.email
}

// または
fun goodExampleWithLet(user: User?) {
    user?.let {
        val name = it.name
        val email = it.email
    }
}

// アンチパターン 2: 過度なネストされたスコープ関数

// 悪い例
fun badNesting() {
    user?.let {
        it.address?.let { address ->
            address.city?.let { city ->
                city.name?.let { name ->
                    println(name)
                }
            }
        }
    }
}

// 良い例
fun goodNesting() {
    val cityName = user?.address?.city?.name
    cityName?.let { println(it) }
}

// または early return
fun goodWithEarlyReturn() {
    val user = user ?: return
    val address = user.address ?: return
    val city = address.city ?: return
    val name = city.name ?: return
    println(name)
}

// アンチパターン 3: 可変性の濫用

// 悪い例
class BadUser {
    var name: String = ""
    var email: String = ""
    var age: Int = 0
    var status: String = ""
}

// 良い例
data class GoodUser(
    val name: String,
    val email: String,
    val age: Int,
    val status: UserStatus
)

// 必要な場合のみ可変に
class UserProfile {
    val id: Long
    var displayName: String
        private set

    constructor(id: Long, displayName: String) {
        this.id = id
        this.displayName = displayName
    }

    fun updateDisplayName(newName: String) {
        displayName = newName
    }
}

// アンチパターン 4: 意味のない変数名

// 悪い例
fun bad(a: Int, b: Int): Int {
    val x = a * b
    val y = x / 2
    return y
}

// 良い例
fun calculateAverageArea(width: Int, height: Int): Int {
    val totalArea = width * height
    val averageArea = totalArea / 2
    return averageArea
}

// アンチパターン 5: 長すぎる関数

// 悪い例：100行以上の関数
fun processOrderBad(order: Order) {
    // バリデーション（20行）
    // 在庫チェック（30行）
    // 決済処理（40行）
    // メール送信（20行）
    // ログ記録（10行）
}

// 良い例：責務を分割
fun processOrderGood(order: Order): Result<Order> {
    return runCatching {
        validateOrder(order)
        checkInventory(order)
        processPayment(order)
        sendConfirmationEmail(order)
        logOrderProcessing(order)
        order
    }
}

private fun validateOrder(order: Order) { /* ... */ }
private fun checkInventory(order: Order) { /* ... */ }
private fun processPayment(order: Order) { /* ... */ }
private fun sendConfirmationEmail(order: Order) { /* ... */ }
private fun logOrderProcessing(order: Order) { /* ... */ }

// アンチパターン 6: プラットフォーム型の放置

// 悪い例：Javaから来た型をそのまま使用
fun badPlatformType() {
    val javaService = JavaService()
    val data = javaService.data  // String! (プラットフォーム型)
    val length = data.length  // nullならNPE
}

// 良い例：明示的にnull許容型として扱う
fun goodPlatformType() {
    val javaService = JavaService()
    val data: String? = javaService.data
    val length = data?.length ?: 0
}
```

## 9.2 パフォーマンスチューニング

### inline関数の適切な使用

```kotlin
// inline関数のメリット：ラムダのオーバーヘッド削減

// 通常の高階関数（ラムダがオブジェクトとして生成される）
fun normalHigherOrder(block: () -> Unit) {
    println("Before")
    block()
    println("After")
}

// inline関数（呼び出し元に展開される）
inline fun inlineHigherOrder(block: () -> Unit) {
    println("Before")
    block()
    println("After")
}

// 使用例
fun example() {
    // 通常の関数：Functionオブジェクトが生成される
    normalHigherOrder {
        println("Normal")
    }

    // inline関数：展開される
    inlineHigherOrder {
        println("Inline")
    }
    // ↓コンパイル後（概念的に）↓
    // println("Before")
    // println("Inline")
    // println("After")
}

// inline を使うべき場合
// 1. 高階関数で頻繁に呼ばれる
inline fun <T> List<T>.customForEach(action: (T) -> Unit) {
    for (element in this) action(element)
}

// 2. reified型パラメータが必要
inline fun <reified T> isInstance(value: Any): Boolean {
    return value is T
}

// 3. 非ローカルリターンが必要
inline fun performAction(action: () -> Unit) {
    action()
}

fun caller() {
    performAction {
        return  // callerから抜ける（inline だから可能）
    }
}

// inline を使うべきでない場合
// 1. 関数が大きい場合（コードサイズが増大）
// 悪い例
inline fun largeFunction(block: () -> Unit) {
    // 100行以上のコード
    block()
}

// 2. ラムダを保持する場合
class BadExample {
    // inline関数内でラムダを保持できない
    // inline fun store(block: () -> Unit) {
    //     this.callback = block  // コンパイルエラー
    // }
}

// noinline の使用
inline fun mixed(
    inline block1: () -> Unit,
    noinline block2: () -> Unit
) {
    block1()  // インライン化
    callbacks.add(block2)  // 保持可能
}

// crossinline の使用
inline fun runWithLogging(crossinline block: () -> Unit) {
    log("Starting")
    thread {
        block()  // 非ローカルリターン禁止
    }
    log("Finished")
}
```

### プリミティブ型の最適化

```kotlin
// Kotlinの数値型は必要に応じてプリミティブ型に最適化される

// プリミティブ型として扱われる
fun primitiveExample() {
    val i: Int = 42  // Java の int
    val l: Long = 42L  // Java の long
    val d: Double = 3.14  // Java の double
}

// ボックス化される場合
fun boxedExample() {
    val nullable: Int? = 42  // Java の Integer
    val list: List<Int> = listOf(1, 2, 3)  // List<Integer>
    val generic: (Int) -> Int = { it * 2 }  // Integer
}

// パフォーマンス最適化のベストプラクティス

// 1. プリミティブ配列の使用
val intArray = IntArray(1000)  // int[]
val doubleArray = DoubleArray(1000)  // double[]

// より遅い（ボックス化）
val boxedArray = Array<Int>(1000) { 0 }  // Integer[]

// 2. インライン value class（Kotlin 1.5+）
@JvmInline
value class UserId(val value: Long)

// 実行時にはLongとして扱われる（オーバーヘッドなし）
fun fetchUser(userId: UserId): User {
    // userIdは実行時にはlong型
    return repository.findById(userId.value)
}

// 3. シーケンスの活用（大きなコレクション）
val largeList = (1..1_000_000).toList()

// 遅い：中間リストが生成される
val result1 = largeList
    .filter { it % 2 == 0 }
    .map { it * 2 }
    .take(10)

// 速い：遅延評価
val result2 = largeList.asSequence()
    .filter { it % 2 == 0 }
    .map { it * 2 }
    .take(10)
    .toList()

// 4. lateinit の適切な使用
class ViewModel {
    // nullableよりlateinitの方が効率的
    private lateinit var repository: Repository

    fun initialize(repo: Repository) {
        repository = repo
    }

    // アクセス時にnullチェック不要
    fun loadData() {
        repository.fetchData()  // nullチェックなし
    }
}

// 5. デリゲートプロパティの適切な使用
class Configuration {
    // lazyは初回アクセス時のみ初期化
    val heavyObject by lazy {
        createHeavyObject()
    }

    // 毎回計算される（キャッシュなし）
    val calculated: Int
        get() = expensiveCalculation()

    // 計算結果をキャッシュ
    val cached: Int by lazy {
        expensiveCalculation()
    }
}
```

### コレクション操作の効率化

```kotlin
// 1. 適切なコレクション型の選択

// リスト：順序が重要、重複あり
val userList = mutableListOf<User>()
userList.add(user)  // O(1) 末尾追加
userList[0]  // O(1) インデックスアクセス
userList.contains(user)  // O(n) 線形探索

// セット：重複なし、順序不要
val userSet = mutableSetOf<User>()
userSet.add(user)  // O(1) 追加
userSet.contains(user)  // O(1) 検索

// マップ：キーによる高速検索
val userMap = mutableMapOf<Long, User>()
userMap[userId] = user  // O(1) 追加
userMap[userId]  // O(1) 検索

// 2. 不要なコピーを避ける

// 悪い例：不要なコピー
fun bad(items: List<String>): List<String> {
    return items
        .toList()  // 不要なコピー
        .filter { it.isNotEmpty() }
        .toList()  // 不要なコピー
}

// 良い例
fun good(items: List<String>): List<String> {
    return items.filter { it.isNotEmpty() }
}

// 3. シーケンスの活用（大規模データ）

// コレクション：即時評価（中間リスト生成）
fun withCollection(items: List<Int>) {
    items
        .filter { it % 2 == 0 }  // 中間リスト1
        .map { it * 2 }          // 中間リスト2
        .filter { it > 10 }      // 中間リスト3
        .forEach { println(it) }
}

// シーケンス：遅延評価（中間リストなし）
fun withSequence(items: List<Int>) {
    items.asSequence()
        .filter { it % 2 == 0 }
        .map { it * 2 }
        .filter { it > 10 }
        .forEach { println(it) }
}

// 4. 事前サイズ指定

// 悪い例：リサイズのオーバーヘッド
fun bad() {
    val list = mutableListOf<Int>()
    repeat(10000) {
        list.add(it)
    }
}

// 良い例：初期容量を指定
fun good() {
    val list = ArrayList<Int>(10000)
    repeat(10000) {
        list.add(it)
    }
}

// 5. 適切な終端操作の選択

// 悪い例：不要な中間変換
val sum1 = items.map { it.price }.sum()

// 良い例：直接集計
val sum2 = items.sumOf { it.price }

// 悪い例
val first1 = items.filter { it.active }.first()

// 良い例
val first2 = items.first { it.active }

// 6. 存在チェックの最適化

// 悪い例：全要素をフィルタリング
val hasActive1 = items.filter { it.active }.isNotEmpty()

// 良い例：最初の一致で停止
val hasActive2 = items.any { it.active }

// 悪い例
val allValid1 = items.filter { it.isValid }.size == items.size

// 良い例
val allValid2 = items.all { it.isValid }
```

## 9.3 エラーハンドリング

### 例外 vs Result型

```kotlin
// 例外ベースのアプローチ
fun fetchUserByIdException(id: Long): User {
    if (id <= 0) {
        throw IllegalArgumentException("Invalid user ID")
    }

    val user = repository.findById(id)
        ?: throw UserNotFoundException("User not found: $id")

    return user
}

// 使用例
fun useException() {
    try {
        val user = fetchUserByIdException(1)
        println("User: ${user.name}")
    } catch (e: UserNotFoundException) {
        println("User not found")
    } catch (e: IllegalArgumentException) {
        println("Invalid input")
    }
}

// Result型ベースのアプローチ
fun fetchUserByIdResult(id: Long): Result<User> {
    return runCatching {
        require(id > 0) { "Invalid user ID" }
        repository.findById(id) ?: error("User not found: $id")
    }
}

// 使用例
fun useResult() {
    fetchUserByIdResult(1)
        .onSuccess { user ->
            println("User: ${user.name}")
        }
        .onFailure { error ->
            when (error) {
                is IllegalArgumentException -> println("Invalid input")
                else -> println("Error: ${error.message}")
            }
        }
}

// カスタムResult型
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
}

fun fetchUserCustom(id: Long): ApiResult<User> {
    return try {
        val user = repository.findById(id)
        if (user != null) {
            ApiResult.Success(user)
        } else {
            ApiResult.Error(404, "User not found")
        }
    } catch (e: NetworkException) {
        ApiResult.NetworkError
    } catch (e: Exception) {
        ApiResult.Error(500, e.message ?: "Unknown error")
    }
}

// 使用例
fun useCustomResult() {
    when (val result = fetchUserCustom(1)) {
        is ApiResult.Success -> println("User: ${result.data.name}")
        is ApiResult.Error -> println("Error ${result.code}: ${result.message}")
        ApiResult.NetworkError -> println("Network error")
    }
}

// どちらを使うべきか？

// 例外を使うべき場合：
// - プログラミングエラー（バグ）
// - 回復不可能なエラー
// - Javaライブラリとの互換性

fun validateInput(input: String) {
    require(input.isNotBlank()) { "Input must not be blank" }
    check(input.length <= 100) { "Input too long" }
}

// Result型を使うべき場合：
// - ビジネスロジックの失敗
// - 予期されるエラー
// - エラーが値の一部

suspend fun login(email: String, password: String): Result<User> {
    return runCatching {
        authenticateUser(email, password)
    }
}
```

### runCatchingの活用

```kotlin
// 基本的な使用
fun basicRunCatching() {
    val result = runCatching {
        riskyOperation()
    }

    result
        .onSuccess { value -> println("Success: $value") }
        .onFailure { error -> println("Error: ${error.message}") }
}

// mapとgetOrElse
fun mapResult() {
    val result = runCatching {
        fetchData()
    }
    .map { data ->
        data.uppercase()
    }
    .getOrElse { "default" }
}

// recover - エラーからの回復
fun recoverFromError() {
    val result = runCatching {
        fetchData()
    }
    .recover { error ->
        when (error) {
            is NetworkException -> "cached-data"
            else -> throw error
        }
    }
    .getOrThrow()
}

// mapCatching - 変換中の例外もキャッチ
fun mapCatchingExample() {
    val result = runCatching {
        "123"
    }
    .mapCatching { str ->
        str.toInt() * 2  // toIntが失敗する可能性
    }
    .getOrElse { 0 }
}

// 実践的な例：複数の操作の連鎖
suspend fun processUserData(userId: Long): Result<ProcessedData> {
    return runCatching {
        val user = fetchUser(userId)
        val profile = fetchProfile(user.profileId)
        val posts = fetchPosts(userId)

        ProcessedData(
            userName = user.name,
            bio = profile.bio,
            postCount = posts.size
        )
    }
    .onFailure { error ->
        log.error("Failed to process user data", error)
    }
}

// エラーの詳細な処理
fun detailedErrorHandling(id: Long): Result<User> {
    return runCatching {
        require(id > 0) { "Invalid ID" }
        val user = repository.findById(id)
            ?: error("User not found")
        check(user.isActive) { "User is inactive" }
        user
    }
    .onFailure { error ->
        when (error) {
            is IllegalArgumentException -> log.warn("Validation error", error)
            is IllegalStateException -> log.warn("Business rule violation", error)
            else -> log.error("Unexpected error", error)
        }
    }
}
```

### カスタムエラー処理の実装

```kotlin
// カスタム例外階層
sealed class AppException(message: String) : Exception(message)

class ValidationException(message: String) : AppException(message)
class BusinessRuleException(message: String) : AppException(message)
class DataNotFoundException(message: String) : AppException(message)
class ExternalServiceException(message: String, cause: Throwable? = null) :
    AppException(message)

// エラーハンドラー
class ErrorHandler {
    fun handle(error: Throwable): ErrorResponse {
        return when (error) {
            is ValidationException -> ErrorResponse(
                code = "VALIDATION_ERROR",
                message = error.message ?: "Validation failed",
                status = 400
            )
            is BusinessRuleException -> ErrorResponse(
                code = "BUSINESS_RULE_VIOLATION",
                message = error.message ?: "Business rule violated",
                status = 422
            )
            is DataNotFoundException -> ErrorResponse(
                code = "NOT_FOUND",
                message = error.message ?: "Resource not found",
                status = 404
            )
            is ExternalServiceException -> ErrorResponse(
                code = "EXTERNAL_SERVICE_ERROR",
                message = "External service unavailable",
                status = 503
            )
            else -> ErrorResponse(
                code = "INTERNAL_ERROR",
                message = "Internal server error",
                status = 500
            )
        }
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val status: Int
)

// エラーコンテキスト
class ErrorContext(
    val operation: String,
    val userId: Long?,
    val additionalInfo: Map<String, Any> = emptyMap()
)

// リトライ機能付きエラーハンドラー
suspend fun <T> executeWithRetry(
    maxAttempts: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    retryOn: (Throwable) -> Boolean = { true },
    block: suspend () -> T
): Result<T> {
    var currentDelay = initialDelay
    var lastException: Throwable? = null

    repeat(maxAttempts) { attempt ->
        try {
            return Result.success(block())
        } catch (e: Throwable) {
            lastException = e

            if (!retryOn(e) || attempt == maxAttempts - 1) {
                return Result.failure(e)
            }

            log.warn("Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms", e)
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }

    return Result.failure(lastException ?: Exception("Unknown error"))
}

// 使用例
suspend fun fetchDataWithRetry(): Result<Data> {
    return executeWithRetry(
        maxAttempts = 3,
        retryOn = { it is NetworkException }
    ) {
        fetchDataFromApi()
    }
}

// グローバルエラーハンドラー
object GlobalErrorHandler {
    private val handlers = mutableMapOf<KClass<out Throwable>, (Throwable) -> Unit>()

    fun <T : Throwable> register(
        errorClass: KClass<T>,
        handler: (T) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        handlers[errorClass] = handler as (Throwable) -> Unit
    }

    fun handle(error: Throwable) {
        val handler = handlers[error::class]
            ?: handlers.entries.firstOrNull { (clazz, _) ->
                clazz.isInstance(error)
            }?.value

        handler?.invoke(error) ?: defaultHandler(error)
    }

    private fun defaultHandler(error: Throwable) {
        log.error("Unhandled error", error)
    }
}

// 登録
fun setupErrorHandlers() {
    GlobalErrorHandler.register(ValidationException::class) { error ->
        log.warn("Validation error: ${error.message}")
    }

    GlobalErrorHandler.register(DataNotFoundException::class) { error ->
        log.info("Data not found: ${error.message}")
    }

    GlobalErrorHandler.register(ExternalServiceException::class) { error ->
        log.error("External service error", error)
        notifyOpsTeam(error)
    }
}
```

## まとめ

本章では、Kotlinのベストプラクティスとイディオムについて学びました：

✅ **命名規約とコーディング規約の遵守**
✅ **Kotlinらしいイディオムの活用**
✅ **inline関数とプリミティブ型の最適化**
✅ **効率的なコレクション操作**
✅ **適切なエラーハンドリング戦略**

これらのプラクティスを適用することで、読みやすく、保守しやすく、パフォーマンスの良いKotlinコードを書くことができます。

## 演習問題

1. 以下のコードをKotlinのイディオムを使って改善してください：
```kotlin
fun processUsers(users: List<User>?): List<String> {
    if (users == null || users.isEmpty()) {
        return emptyList()
    }
    val result = mutableListOf<String>()
    for (user in users) {
        if (user.age >= 18 && user.isActive) {
            result.add(user.name.toUpperCase())
        }
    }
    return result.sorted()
}
```

2. シーケンスを使って、100万要素のリストから条件に合う最初の10要素を効率的に取得する関数を実装してください。

3. カスタムResult型とrunCatchingを組み合わせて、APIエラー（ネットワークエラー、認証エラー、サーバーエラー）を適切にハンドリングする関数を実装してください。