# 第5章：Null安全性 - NullPointerExceptionからの解放

## 5.1 Nullable型の扱い

### Null安全性の基本

Kotlinの最も重要な特徴の一つは、Null安全性を型システムに組み込んでいることです。これにより、多くのNullPointerExceptionをコンパイル時に防ぐことができます。

```kotlin
// Non-nullable型（デフォルト）
var name: String = "Kotlin"
// name = null  // コンパイルエラー！

// Nullable型（?を付ける）
var nullableName: String? = "Kotlin"
nullableName = null  // OK

// Nullable型へのアクセスはチェックが必要
val length = nullableName.length  // コンパイルエラー！
```

**Javaとの比較：**

```java
// Java - すべての参照型がnull可能
String name = "Java";
name = null;  // コンパイル可能

// 実行時にNullPointerException
int length = name.length();  // NPE!

// @NonNull/@Nullableアノテーション（オプショナル）
@NonNull String nonNullName = "Java";
@Nullable String nullableName = null;
```

### 安全呼び出し演算子（?.）

安全呼び出し演算子は、レシーバーがnullの場合に自動的にnullを返します。

```kotlin
val name: String? = "Kotlin"
val length: Int? = name?.length  // nameがnullなら length もnull

// チェーン呼び出し
val country: String? = user?.address?.country?.name
// userまたはaddressまたはcountryがnullならcountryはnull

// メソッド呼び出し
val upperCase: String? = name?.uppercase()

// より実践的な例
data class Company(val name: String, val address: Address?)
data class Address(val city: String, val country: Country?)
data class Country(val name: String, val code: String)

val company: Company? = getCompany()
val countryCode: String? = company?.address?.country?.code

// 安全呼び出しとlet
company?.address?.let { address ->
    println("Address: ${address.city}")
}
```

**Javaとの比較：**

```java
// Java - 手動のnullチェックが必要
Company company = getCompany();
String countryCode = null;
if (company != null) {
    Address address = company.getAddress();
    if (address != null) {
        Country country = address.getCountry();
        if (country != null) {
            countryCode = country.getCode();
        }
    }
}

// Java 8以降のOptional
Optional<Company> company = Optional.ofNullable(getCompany());
Optional<String> countryCode = company
    .flatMap(c -> Optional.ofNullable(c.getAddress()))
    .flatMap(a -> Optional.ofNullable(a.getCountry()))
    .map(c -> c.getCode());
```

### エルビス演算子（?:）

エルビス演算子は、左辺がnullの場合に右辺の値を返します。

```kotlin
// 基本的な使い方
val name: String? = null
val displayName: String = name ?: "Unknown"
println(displayName)  // "Unknown"

// デフォルト値の提供
fun greet(name: String?) {
    val actualName = name ?: "Guest"
    println("Hello, $actualName!")
}

greet("Alice")  // Hello, Alice!
greet(null)     // Hello, Guest!

// 安全呼び出しと組み合わせ
val length: Int = name?.length ?: 0

// early returnとの組み合わせ
fun processUser(user: User?) {
    val validUser = user ?: run {
        println("User is null")
        return
    }
    // validUserは非null型として扱える
    println("Processing ${validUser.name}")
}

// 例外のスロー
fun requireUser(user: User?): User {
    return user ?: throw IllegalArgumentException("User must not be null")
}

// より実践的な例
data class Config(
    val host: String? = null,
    val port: Int? = null,
    val timeout: Int? = null
)

fun createConnection(config: Config) {
    val host = config.host ?: "localhost"
    val port = config.port ?: 8080
    val timeout = config.timeout ?: 30000

    println("Connecting to $host:$port (timeout: ${timeout}ms)")
}
```

### 非null表明（!!）の適切な使用

非null表明演算子は、nullableな値を強制的に非null型に変換しますが、使用には注意が必要です。

```kotlin
val name: String? = "Kotlin"
val length: Int = name!!.length  // nameがnullならNPEが発生

// !! の不適切な使用例（避けるべき）
fun badExample(user: User?) {
    val name = user!!.name  // user がnullならNPE
    val email = user!!.email  // 2回もチェック（冗長）
}

// より良いアプローチ
fun goodExample(user: User?) {
    val user = user ?: return  // early return
    val name = user.name  // userは非null型
    val email = user.email
}

// !! を使うべき場面
class MyClass {
    private var _data: String? = null

    fun initialize(data: String) {
        _data = data
    }

    fun process() {
        // initialize()が必ず先に呼ばれることが保証されている場合
        val data = _data!!
        println(data)
    }
}

// より安全な代替案：lateinit
class MyClassBetter {
    private lateinit var data: String

    fun initialize(data: String) {
        this.data = data
    }

    fun process() {
        // lateinitの初期化チェック
        if (::data.isInitialized) {
            println(data)
        } else {
            println("Data not initialized")
        }
    }
}
```

### let、run、also、applyとnull処理

```kotlin
// let - nullable値の変換と処理
val name: String? = "Kotlin"
name?.let {
    println("Name length: ${it.length}")
    println("Uppercase: ${it.uppercase()}")
}

// letを使ったnullチェックとearly return
fun processUser(user: User?) {
    user?.let {
        println("Processing user: ${it.name}")
        sendEmail(it.email)
        updateDatabase(it)
    } ?: println("User is null")
}

// 複数のnullable値の処理
val firstName: String? = "John"
val lastName: String? = "Doe"

val fullName = firstName?.let { first ->
    lastName?.let { last ->
        "$first $last"
    }
} ?: "Unknown"

// run - オブジェクトのコンフィグと計算
val result = config?.run {
    connect()
    authenticate()
    fetchData()
} ?: emptyList()

// also - デバッグとロギング
val processed = data
    ?.also { println("Original: $it") }
    ?.process()
    ?.also { println("Processed: $it") }

// apply - オブジェクトの初期化
val user = User().apply {
    name = "Alice"
    email = "alice@example.com"
}
```

## 5.2 スマートキャストとタイプチェック

### is演算子と自動キャスト

Kotlinは型チェック後、自動的にキャストを行います。

```kotlin
// 基本的なスマートキャスト
fun describe(x: Any): String {
    return when (x) {
        is String -> "String of length ${x.length}"  // 自動的にStringにキャスト
        is Int -> "Integer: ${x * 2}"
        is List<*> -> "List of size ${x.size}"
        else -> "Unknown type"
    }
}

// nullチェック後のスマートキャスト
fun printLength(str: String?) {
    if (str != null) {
        // このブロック内では str は非null型として扱われる
        println(str.length)
    }
}

// 複合条件でのスマートキャスト
fun processValue(value: Any?) {
    if (value != null && value is String) {
        // valueは非null のString型として扱われる
        println(value.uppercase())
    }
}

// when式でのスマートキャスト
sealed class Result {
    data class Success(val data: String) : Result()
    data class Error(val message: String) : Result()
    object Loading : Result()
}

fun handle(result: Result) {
    when (result) {
        is Result.Success -> println(result.data)  // data に直接アクセス
        is Result.Error -> println(result.message)  // message に直接アクセス
        Result.Loading -> println("Loading...")
    }
}

// スマートキャストが機能しない場合
class Container {
    var value: Any? = null

    fun process() {
        if (value is String) {
            // エラー！varプロパティは他のスレッドで変更される可能性がある
            // println(value.length)
        }
    }
}

// 解決策：ローカル変数にコピー
class ContainerFixed {
    var value: Any? = null

    fun process() {
        val localValue = value
        if (localValue is String) {
            println(localValue.length)  // OK
        }
    }
}
```

**Javaとの比較：**

```java
// Java - 明示的なキャストが必要
public String describe(Object x) {
    if (x instanceof String) {
        String str = (String) x;  // 明示的キャスト
        return "String of length " + str.length();
    } else if (x instanceof Integer) {
        Integer i = (Integer) x;
        return "Integer: " + (i * 2);
    }
    return "Unknown type";
}

// Java 16以降のパターンマッチング
public String describe(Object x) {
    if (x instanceof String str) {
        return "String of length " + str.length();
    } else if (x instanceof Integer i) {
        return "Integer: " + (i * 2);
    }
    return "Unknown type";
}
```

### as演算子と安全なキャスト（as?）

```kotlin
// 通常のキャスト（as）
val obj: Any = "Kotlin"
val str: String = obj as String
println(str.length)

// キャストに失敗すると ClassCastException
val obj2: Any = 123
// val str2: String = obj2 as String  // ClassCastException!

// 安全なキャスト（as?）
val obj3: Any = 123
val str3: String? = obj3 as? String  // キャストに失敗すると null
println(str3)  // null

// 実践的な使用例
fun processString(value: Any) {
    val str = value as? String ?: run {
        println("Value is not a string")
        return
    }
    println("Processing: ${str.uppercase()}")
}

// コレクションのキャスト
fun processItems(items: Any) {
    val stringList = items as? List<String> ?: emptyList()
    stringList.forEach { println(it.uppercase()) }
}

// 安全なキャストとlet
fun safeCast(value: Any) {
    (value as? String)?.let { str ->
        println("String value: $str")
    }

    (value as? Int)?.let { num ->
        println("Integer value: ${num * 2}")
    }
}

// 型パラメータとキャスト
inline fun <reified T> safeCastList(items: List<*>): List<T>? {
    return items.all { it is T }.let { allMatch ->
        if (allMatch) {
            @Suppress("UNCHECKED_CAST")
            items as List<T>
        } else {
            null
        }
    }
}
```

### 契約（Contract）による型の絞り込み

Kotlin 1.3以降、契約を使用してコンパイラに追加の型情報を提供できます。

```kotlin
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// 標準ライブラリの require/check は契約を使用している
fun processUser(user: User?) {
    require(user != null) { "User must not be null" }
    // この行以降、userは非null型として扱われる
    println(user.name)
}

fun validateAge(age: Int?) {
    checkNotNull(age) { "Age must not be null" }
    // この行以降、ageは非null型
    require(age >= 0) { "Age must be non-negative" }
}

// カスタム契約の定義
@OptIn(ExperimentalContracts::class)
fun isValidString(value: String?): Boolean {
    contract {
        returns(true) implies (value != null)
    }
    return value != null && value.isNotBlank()
}

fun processValue(value: String?) {
    if (isValidString(value)) {
        // valueは非null型として扱われる
        println(value.uppercase())
    }
}

// 複雑な契約の例
@OptIn(ExperimentalContracts::class)
fun isNonEmptyList(list: List<*>?): Boolean {
    contract {
        returns(true) implies (list != null)
    }
    return list != null && list.isNotEmpty()
}

fun processItems(items: List<String>?) {
    if (isNonEmptyList(items)) {
        // itemsは非null のList<String>として扱われる
        println("First item: ${items.first()}")
    }
}
```

## 5.3 実践的なNull処理パターン

### Optionalとの比較

```kotlin
// Java Optional の例
// Optional<String> findUserName(long id)

// Kotlinでの等価なコード
fun findUserName(id: Long): String? {
    // データベース検索など
    return if (id > 0) "User $id" else null
}

// Optionalの主な操作のKotlin版
// Java: optional.map(String::toUpperCase)
val userName: String? = findUserName(1)
val upperCase: String? = userName?.uppercase()

// Java: optional.orElse("default")
val name: String = userName ?: "default"

// Java: optional.orElseGet(() -> computeDefault())
val name2: String = userName ?: computeDefault()

// Java: optional.orElseThrow()
val name3: String = userName ?: throw NoSuchElementException()

// Java: optional.ifPresent(System.out::println)
userName?.let { println(it) }

// Java: optional.filter(s -> s.length() > 5)
val filtered: String? = userName?.takeIf { it.length > 5 }

// 複雑な変換
// Java:
// optional.map(String::trim)
//         .filter(s -> !s.isEmpty())
//         .map(String::toUpperCase)

// Kotlin:
val result: String? = userName
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.uppercase()
```

### Null許容型を返すAPIの扱い方

```kotlin
// リポジトリパターン
interface UserRepository {
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun findAll(): List<User>  // 空リストを返す、nullではない
}

class UserService(private val repository: UserRepository) {
    // パターン1: nullを返す
    fun getUserById(id: Long): User? {
        return repository.findById(id)
    }

    // パターン2: Resultを返す
    fun getUserByIdSafe(id: Long): Result<User> {
        return repository.findById(id)
            ?.let { Result.success(it) }
            ?: Result.failure(UserNotFoundException(id))
    }

    // パターン3: 例外をスロー
    fun requireUserById(id: Long): User {
        return repository.findById(id)
            ?: throw UserNotFoundException(id)
    }

    // パターン4: デフォルト値を返す
    fun getUserOrGuest(id: Long): User {
        return repository.findById(id) ?: createGuestUser()
    }

    // パターン5: シールドクラスを使用
    fun fetchUser(id: Long): UserResult {
        return repository.findById(id)
            ?.let { UserResult.Found(it) }
            ?: UserResult.NotFound(id)
    }

    private fun createGuestUser() = User(0, "Guest", "guest@example.com")
}

sealed class UserResult {
    data class Found(val user: User) : UserResult()
    data class NotFound(val id: Long) : UserResult()
}

class UserNotFoundException(id: Long) : Exception("User not found: $id")

// 使用例
fun handleUser(service: UserService, id: Long) {
    // パターン1の使用
    val user = service.getUserById(id)
    user?.let { println("Found: ${it.name}") }
        ?: println("User not found")

    // パターン2の使用
    service.getUserByIdSafe(id)
        .onSuccess { println("Success: ${it.name}") }
        .onFailure { println("Error: ${it.message}") }

    // パターン5の使用
    when (val result = service.fetchUser(id)) {
        is UserResult.Found -> println("User: ${result.user.name}")
        is UserResult.NotFound -> println("Not found: ${result.id}")
    }
}
```

### レガシーJavaコードとの連携時の考慮点

```kotlin
// プラットフォーム型（JavaからのNull許容性が不明な型）
// Javaコード：
// public String getName() { return name; }  // @Nullable/@NonNull がない

// Kotlinから使用する場合
val javaObject = JavaClass()
val name: String = javaObject.name  // プラットフォーム型 String!

// 安全な扱い方
val safeName: String? = javaObject.name  // nullable として扱う

// Javaメソッドのラップ
class KotlinWrapper(private val javaService: JavaLegacyService) {
    // Javaの戻り値をnullableに変換
    fun findUser(id: Long): User? {
        return try {
            javaService.findUser(id)  // nullまたはUserを返す可能性
        } catch (e: Exception) {
            null
        }
    }

    // Javaの例外をResultに変換
    fun getUserSafe(id: Long): Result<User> {
        return runCatching {
            val user = javaService.getUser(id)
            requireNotNull(user) { "User not found" }
        }
    }

    // リストの安全な処理
    fun getAllUsers(): List<User> {
        return javaService.getAllUsers()?.filterNotNull() ?: emptyList()
    }
}

// Javaアノテーションの活用
// Javaコード：
// @Nullable public String findEmail(long id)
// @NonNull public String getEmail(long id)

// Kotlinでの扱い
fun useJavaApi(api: JavaApi) {
    val email1: String? = api.findEmail(1)  // @Nullable -> String?
    val email2: String = api.getEmail(1)    // @NonNull -> String
}

// Java コレクションの安全な変換
fun processJavaList(javaList: java.util.List<String>?) {
    // Javaのリストをimmutableなリストに変換
    val kotlinList: List<String> = javaList?.toList() ?: emptyList()

    // null要素をフィルタリング
    val nonNullList: List<String> = javaList?.filterNotNull() ?: emptyList()
}

// Java8のOptionalとの相互運用
fun processOptional(optional: java.util.Optional<String>) {
    // OptionalをnullableとOptionalle型に変換
    val value: String? = optional.orElse(null)

    // または
    val value2: String? = if (optional.isPresent) optional.get() else null
}

// 拡張関数による変換
fun <T> java.util.Optional<T>.toNullable(): T? = orElse(null)

fun useOptional(optional: java.util.Optional<String>) {
    val value: String? = optional.toNullable()
    value?.let { println(it) }
}
```

### 実践的なNull処理のベストプラクティス

```kotlin
// 1. nullable型は必要最小限に
// 悪い例
data class User(
    val id: Long?,
    val name: String?,
    val email: String?
)

// 良い例
data class User(
    val id: Long,
    val name: String,
    val email: String
)

// 2. Early Returnを活用
// 悪い例
fun processUser(user: User?) {
    if (user != null) {
        if (user.isActive) {
            if (user.email != null) {
                sendEmail(user.email)
            }
        }
    }
}

// 良い例
fun processUser(user: User?) {
    val validUser = user ?: return
    if (!validUser.isActive) return
    val email = validUser.email ?: return
    sendEmail(email)
}

// 3. requireやcheckを活用
fun createOrder(userId: Long?, items: List<OrderItem>?) {
    requireNotNull(userId) { "User ID must not be null" }
    requireNotNull(items) { "Items must not be null" }
    require(items.isNotEmpty()) { "Order must contain at least one item" }

    // この行以降、userId と items は非null型
    val order = Order(userId, items)
}

// 4. デフォルト値の提供
data class Config(
    val host: String? = null,
    val port: Int? = null
) {
    fun getHost(): String = host ?: "localhost"
    fun getPort(): Int = port ?: 8080
}

// 5. null許容型の連鎖は避ける
// 悪い例
val country: String? = user?.address?.city?.country?.name

// 良い例（中間結果を検証）
fun getCountryName(user: User?): String? {
    val user = user ?: return null
    val address = user.address ?: return null
    val city = address.city ?: return null
    val country = city.country ?: return null
    return country.name
}

// または let を使用
fun getCountryName2(user: User?): String? {
    return user?.address?.city?.country?.let { country ->
        country.name.takeIf { it.isNotBlank() }
    }
}

// 6. コレクションのnull処理
fun processUsers(users: List<User>?) {
    // nullまたは空の場合は早期リターン
    if (users.isNullOrEmpty()) return

    users.forEach { user ->
        println(user.name)
    }
}

// 7. ラムダでのnull処理
fun findUser(id: Long): User? = TODO()

fun processUserById(id: Long) {
    findUser(id)?.also { user ->
        println("Found user: ${user.name}")
        updateLastAccess(user)
    } ?: println("User not found")
}
```

## まとめ

本章では、KotlinのNull安全性について学びました：

✅ **Nullable型と非Nullable型の明確な区別**
✅ **安全呼び出し演算子（?.）とエルビス演算子（?:）**
✅ **スマートキャストによる型の自動変換**
✅ **安全なキャスト（as?）による安全な型変換**
✅ **実践的なNull処理パターンとベストプラクティス**

KotlinのNull安全性により、NullPointerExceptionの多くをコンパイル時に防ぐことができ、より安全なコードを書くことができます。

## 演習問題

1. 以下のJavaコードをKotlinの安全なNull処理を使って書き直してください：

```java
public String getUserEmail(Database db, long userId) {
    User user = db.findUser(userId);
    if (user != null) {
        Profile profile = user.getProfile();
        if (profile != null) {
            Email email = profile.getEmail();
            if (email != null) {
                return email.getAddress();
            }
        }
    }
    return "no-email@example.com";
}
```

2. シールドクラスを使って、API呼び出しの結果を表現する型を作成してください。成功（データ）、エラー（エラーメッセージ）、ネットワークエラー、タイムアウトの4つのケースを表現し、それぞれを処理する関数を実装してください。

3. nullable なリストから、null要素を除去し、条件を満たす要素のみを抽出する関数を実装してください。例：`List<String?>?` から長さが5以上の文字列のみを取り出す。