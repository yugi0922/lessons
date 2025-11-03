# 第4章：オブジェクト指向プログラミング - Kotlinスタイル

## 4.1 クラスとプロパティ

### プライマリコンストラクタ

Kotlinのクラス定義は、Javaと比べて非常に簡潔です。プライマリコンストラクタをクラス宣言に直接記述できます。

```kotlin
// Kotlin - プライマリコンストラクタ
class Person(val name: String, var age: Int)

// 使用例
val person = Person("Alice", 25)
println(person.name)  // Alice
person.age = 26  // varなので変更可能
```

**Javaとの比較：**

```java
// Java - 冗長な記述
public class Person {
    private final String name;
    private int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```

### initブロックと初期化

```kotlin
class User(val name: String, val email: String) {
    val registrationDate: Long

    init {
        // 初期化処理
        println("Creating user: $name")
        registrationDate = System.currentTimeMillis()
        validateEmail(email)
    }

    init {
        // 複数のinitブロックを持てる
        println("User registration completed")
    }


    private fun validateEmail(email: String) {
        require(email.contains("@")) { "Invalid email format" }
    }
}
```

Javaとの違い
<img width="499" height="191" alt="スクリーンショット 2025-11-04 0 24 38" src="https://github.com/user-attachments/assets/8417d8ef-7189-40ed-a52d-877a6147aebf" />


### セカンダリコンストラクタ

```kotlin
class Rectangle {
    val width: Double
    val height: Double

    // プライマリコンストラクタ
    constructor(width: Double, height: Double) {
        this.width = width
        this.height = height
    }

    // セカンダリコンストラクタ（正方形用）
    constructor(size: Double) : this(size, size)

    // セカンダリコンストラクタ（整数から変換）
    constructor(width: Int, height: Int) : this(width.toDouble(), height.toDouble())
}

// より簡潔な書き方
class Rectangle(val width: Double, val height: Double) {
    // セカンダリコンストラクタはプライマリを呼び出す必要がある
    constructor(size: Double) : this(size, size)
    // 整数から変換
    constructor(width: Int, height: Int) : this(width.toDouble(), height.toDouble())
}
```

### getter/setterの自動生成

Kotlinではプロパティのgetter/setterが自動生成されますが、カスタマイズも可能です。

```kotlin
class Temperature {
    var celsius: Double = 0.0
        set(value) {
            if (value < -273.15) {
                throw IllegalArgumentException("Temperature below absolute zero")
            }
            field = value  // fieldはバッキングフィールド
        }

    val fahrenheit: Double
        get() = celsius * 9/5 + 32  // 計算プロパティ

    var kelvin: Double
        get() = celsius + 273.15
        set(value) {
            celsius = value - 273.15
        }
}

// 使用例
val temp = Temperature()
temp.celsius = 25.0
println(temp.fahrenheit)  // 77.0
temp.kelvin = 300.0
println(temp.celsius)  // 26.85
```

**Javaとの比較：**

```java
// Java
public class Temperature {
    private double celsius = 0.0;

    public void setCelsius(double value) {
        if (value < -273.15) {
            throw new IllegalArgumentException("Temperature below absolute zero");
        }
        this.celsius = value;
    }

    public double getCelsius() {
        return celsius;
    }

    public double getFahrenheit() {
        return celsius * 9/5 + 32;
    }

    public double getKelvin() {
        return celsius + 273.15;
    }

    public void setKelvin(double value) {
        this.celsius = value - 273.15;
    }
}
```

### バッキングフィールド

```kotlin
class Counter {
    var value: Int = 0
        private set  // getterはpublic、setterはprivate

    fun increment() {
        value++
    }
}

class LazyProperty {
    val expensive: String
        get() {
            println("Computing expensive property")
            return "Expensive Value"
        }
}

// 遅延初期化プロパティ
class DatabaseConnection {
    lateinit var connection: Connection

    fun connect() {
        connection = createConnection()
    }

    val isConnected: Boolean
        get() = ::connection.isInitialized
}

// 遅延評価プロパティ
class Configuration {
    val heavyConfig: Config by lazy {
        println("Initializing heavy configuration")
        loadConfiguration()
    }
}
```

## 4.2 継承とインターフェース

### open/final/abstractの明示的な宣言

Kotlinではクラスとメソッドはデフォルトで`final`（継承・オーバーライド不可）です。継承を許可するには`open`を明示する必要があります。

```kotlin
// デフォルトはfinal（継承不可）
class FinalClass {
    fun finalMethod() {}
}

// 継承を許可
open class BaseClass {
    open fun overridableMethod() {
        println("Base implementation")
    }

    fun finalMethod() {
        println("This cannot be overridden")
    }
}

class DerivedClass : BaseClass() {
    override fun overridableMethod() {
        super.overridableMethod()
        println("Derived implementation")
    }

    // finalMethod()はオーバーライドできない
}

// 抽象クラス
abstract class Shape {
    abstract val area: Double
    abstract fun draw()

    open fun describe() {
        println("This is a shape with area $area")
    }
}

class Circle(val radius: Double) : Shape() {
    override val area: Double
        get() = Math.PI * radius * radius

    override fun draw() {
        println("Drawing circle with radius $radius")
    }
}
```

**Javaとの比較：**

```java
// Java - デフォルトで継承可能
public class BaseClass {
    public void overridableMethod() {
        System.out.println("Base implementation");
    }

    public final void finalMethod() {
        System.out.println("This cannot be overridden");
    }
}

public class DerivedClass extends BaseClass {
    @Override
    public void overridableMethod() {
        super.overridableMethod();
        System.out.println("Derived implementation");
    }
}

// 継承を防ぐにはfinalを明示
public final class FinalClass {
    public void finalMethod() {}
}
```

### インターフェースのデフォルト実装

Kotlinのインターフェースはプロパティとデフォルト実装を持つことができます。

```kotlin
interface Clickable {
    fun click()  // 抽象メソッド

    fun showOff() = println("I'm clickable!")  // デフォルト実装
}

interface Focusable {
    fun setFocus(focused: Boolean)

    fun showOff() = println("I'm focusable!")
}

class Button : Clickable, Focusable {
    override fun click() = println("Button clicked")

    override fun setFocus(focused: Boolean) {
        println("Focus set to $focused")
    }

    // 複数のインターフェースで同名のメソッドがある場合は明示的に実装
    override fun showOff() {
        super<Clickable>.showOff()
        super<Focusable>.showOff()
        println("I'm a button!")
    }
}

// プロパティを持つインターフェース
interface Named {
    val name: String  // 抽象プロパティ

    val displayName: String
        get() = name.uppercase()  // 計算プロパティ
}

class Person(override val name: String) : Named
```

### プロパティの委譲

Kotlinはプロパティの振る舞いを委譲できる強力な機能を提供します。

```kotlin
// 標準の委譲：lazy
class HeavyObject {
    val config: Configuration by lazy {
        println("Loading configuration...")
        loadConfiguration()
    }
}

// 標準の委譲：observable
import kotlin.properties.Delegates

class User {
    var name: String by Delegates.observable("<no name>") {
        prop, old, new ->
        println("$old -> $new")
    }
}

val user = User()
user.name = "Alice"  // 出力: <no name> -> Alice
user.name = "Bob"    // 出力: Alice -> Bob

// 標準の委譲：vetoable（条件付き変更）
class Product {
    var price: Double by Delegates.vetoable(0.0) {
        prop, old, new ->
        new >= 0  // 負の値は拒否
    }
}

// Map による委譲
class UserData(map: Map<String, Any?>) {
    val name: String by map
    val age: Int by map
    val email: String by map
}

val userData = UserData(mapOf(
    "name" to "Alice",
    "age" to 25,
    "email" to "alice@example.com"
))

println(userData.name)  // Alice

// カスタム委譲
class LoggingDelegate<T>(private var value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        println("Getting ${property.name}: $value")
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        println("Setting ${property.name}: $value -> $newValue")
        value = newValue
    }
}

class Example {
    var data: String by LoggingDelegate("initial")
}
```

## 4.3 データクラスとシールドクラス

### データクラス

データクラスは、データを保持することが主目的のクラスです。`equals()`, `hashCode()`, `toString()`, `copy()`が自動生成されます。

```kotlin
// データクラスの定義
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val age: Int
)

// 自動生成される機能
val user1 = User(1, "Alice", "alice@example.com", 25)
val user2 = User(1, "Alice", "alice@example.com", 25)

// equals/hashCode
println(user1 == user2)  // true（構造的等価性）

// toString
println(user1)  // User(id=1, name=Alice, email=alice@example.com, age=25)

// copy（一部のプロパティを変更したコピー）
val user3 = user1.copy(age = 26)
println(user3)  // User(id=1, name=Alice, email=alice@example.com, age=26)

// 分解宣言
val (id, name, email, age) = user1
println("$name is $age years old")
```

**Javaとの比較：**

```java
// Java - Recordクラス（Java 14+）
public record User(long id, String name, String email, int age) {}

// Java 13以前 - 手動実装
public class User {
    private final long id;
    private final String name;
    private final String email;
    private final int age;

    public User(long id, String name, String email, int age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // getter, equals, hashCode, toString を手動実装...
    // 約50行以上のボイラープレートコード
}
```

### データクラスの制約と高度な使用

```kotlin
// データクラスはプライマリコンストラクタに少なくとも1つのパラメータが必要
data class Point(val x: Int, val y: Int)

// プロパティにデフォルト値を設定可能
data class Configuration(
    val host: String = "localhost",
    val port: Int = 8080,
    val secure: Boolean = false
)

val config1 = Configuration()
val config2 = Configuration(host = "example.com")
val config3 = Configuration(port = 443, secure = true)

// 分解宣言は最初の5つのプロパティまで
data class Person(val name: String, val age: Int, val city: String)
val (name, age) = Person("Alice", 25, "Tokyo")  // cityは無視される

// コレクション内のデータクラス
val users = listOf(
    User(1, "Alice", "alice@example.com", 25),
    User(2, "Bob", "bob@example.com", 30),
    User(3, "Charlie", "charlie@example.com", 35)
)

val sortedByAge = users.sortedBy { it.age }
val grouped = users.groupBy { it.age / 10 }  // 年代でグループ化
```

### シールドクラス（代数的データ型）

シールドクラスは、限定された継承階層を表現するために使用されます。すべてのサブクラスはコンパイル時に既知である必要があります。

```kotlin
// シールドクラスの定義
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// when式での網羅的チェック
fun <T> handleResult(result: Result<T>) {
    when (result) {
        is Result.Success -> println("Success: ${result.data}")
        is Result.Error -> println("Error: ${result.exception.message}")
        Result.Loading -> println("Loading...")
        // elseは不要（すべてのケースが網羅されている）
    }
}

// 実用例：APIレスポンス
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T, val code: Int = 200) : ApiResponse<T>()
    data class Error(val message: String, val code: Int) : ApiResponse<Nothing>()
    object NetworkError : ApiResponse<Nothing>()
    object Unauthorized : ApiResponse<Nothing>()
}

fun handleApiResponse(response: ApiResponse<User>) {
    when (response) {
        is ApiResponse.Success -> {
            println("User: ${response.data}")
        }
        is ApiResponse.Error -> {
            println("Error ${response.code}: ${response.message}")
        }
        ApiResponse.NetworkError -> {
            println("Network error occurred")
        }
        ApiResponse.Unauthorized -> {
            println("Unauthorized access")
        }
    }
}

// シールドインターフェース（Kotlin 1.5+）
sealed interface Operation {
    data class Add(val a: Int, val b: Int) : Operation
    data class Subtract(val a: Int, val b: Int) : Operation
    data class Multiply(val a: Int, val b: Int) : Operation
    data class Divide(val a: Int, val b: Int) : Operation
}

fun execute(operation: Operation): Int = when (operation) {
    is Operation.Add -> operation.a + operation.b
    is Operation.Subtract -> operation.a - operation.b
    is Operation.Multiply -> operation.a * operation.b
    is Operation.Divide -> operation.a / operation.b
}
```

**Javaとの比較：**

```java
// Java - sealed class（Java 17+）
public sealed interface Result<T>
    permits Success, Error, Loading {
}

public final class Success<T> implements Result<T> {
    private final T data;

    public Success(T data) {
        this.data = data;
    }

    public T getData() { return data; }
}

public final class Error implements Result<Void> {
    private final Exception exception;

    public Error(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() { return exception; }
}

public final class Loading implements Result<Void> {
    public static final Loading INSTANCE = new Loading();
    private Loading() {}
}

// パターンマッチング（Java 17+）
public static <T> void handleResult(Result<T> result) {
    switch (result) {
        case Success<T> s -> System.out.println("Success: " + s.getData());
        case Error e -> System.out.println("Error: " + e.getException().getMessage());
        case Loading l -> System.out.println("Loading...");
    }
}
```

## 4.4 オブジェクト宣言とコンパニオンオブジェクト

### シングルトンパターンの言語レベルサポート

Kotlinでは`object`キーワードを使用してシングルトンを簡単に作成できます。

```kotlin
// オブジェクト宣言（シングルトン）
object DatabaseConfig {
    val url: String = "jdbc:postgresql://localhost:5432/mydb"
    val username: String = "user"

    fun connect() {
        println("Connecting to $url")
    }
}

// 使用例
DatabaseConfig.connect()
println(DatabaseConfig.url)

// より実践的な例：アプリケーション設定
object AppConfig {
    private val properties = mutableMapOf<String, String>()

    fun load(configFile: String) {
        // 設定ファイルの読み込み
    }

    fun get(key: String): String? = properties[key]

    fun set(key: String, value: String) {
        properties[key] = value
    }
}
```

**Javaとの比較：**

```java
// Java - シングルトンパターン
public class DatabaseConfig {
    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private final String url = "jdbc:postgresql://localhost:5432/mydb";
    private final String username = "user";

    private DatabaseConfig() {
        // プライベートコンストラクタ
    }

    public static DatabaseConfig getInstance() {
        return INSTANCE;
    }

    public void connect() {
        System.out.println("Connecting to " + url);
    }

    public String getUrl() { return url; }
    public String getUsername() { return username; }
}

// 使用例
DatabaseConfig.getInstance().connect();
```

### コンパニオンオブジェクト（staticメンバーの代替）

Kotlinには`static`キーワードがありませんが、コンパニオンオブジェクトを使用して同様の機能を実現できます。

```kotlin
class User(val id: Long, val name: String) {
    companion object {
        // 静的プロパティ
        private var nextId = 1L
        const val DEFAULT_ROLE = "user"  // コンパイル時定数

        // 静的メソッド
        fun create(name: String): User {
            return User(nextId++, name)
        }

        // ファクトリーメソッド
        fun fromJson(json: String): User {
            // JSONパース処理
            return User(1, "Parsed User")
        }
    }

    fun info() {
        println("User $id: $name (role: $DEFAULT_ROLE)")
    }
}

// 使用例
val user1 = User.create("Alice")
val user2 = User.create("Bob")
println(User.DEFAULT_ROLE)

// 名前付きコンパニオンオブジェクト
class Database {
    companion object Factory {
        fun connect(url: String): Database {
            return Database()
        }
    }
}

val db = Database.connect("jdbc:...")
// または
val db2 = Database.Factory.connect("jdbc:...")
```

### ファクトリーメソッドの実装

```kotlin
// ファクトリーメソッドパターン
class Color private constructor(
    val red: Int,
    val green: Int,
    val blue: Int
) {
    companion object {
        fun rgb(red: Int, green: Int, blue: Int): Color {
            require(red in 0..255) { "Invalid red value" }
            require(green in 0..255) { "Invalid green value" }
            require(blue in 0..255) { "Invalid blue value" }
            return Color(red, green, blue)
        }

        fun fromHex(hex: String): Color {
            val r = hex.substring(1, 3).toInt(16)
            val g = hex.substring(3, 5).toInt(16)
            val b = hex.substring(5, 7).toInt(16)
            return Color(r, g, b)
        }

        // 事前定義された色
        val RED = Color(255, 0, 0)
        val GREEN = Color(0, 255, 0)
        val BLUE = Color(0, 0, 255)
        val BLACK = Color(0, 0, 0)
        val WHITE = Color(255, 255, 255)
    }

    override fun toString(): String {
        return "Color(R:$red, G:$green, B:$blue)"
    }
}

// 使用例
val color1 = Color.rgb(255, 128, 0)
val color2 = Color.fromHex("#FF8000")
val color3 = Color.RED

// より複雑な例：バリデーション付きファクトリー
sealed class EmailResult {
    data class Valid(val email: Email) : EmailResult()
    data class Invalid(val reason: String) : EmailResult()
}

class Email private constructor(val address: String) {
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

        fun create(address: String): EmailResult {
            return when {
                address.isBlank() ->
                    EmailResult.Invalid("Email cannot be blank")
                !EMAIL_REGEX.matches(address) ->
                    EmailResult.Invalid("Invalid email format")
                else ->
                    EmailResult.Valid(Email(address))
            }
        }

        // 検証なし（内部使用）
        internal fun createUnsafe(address: String) = Email(address)
    }

    override fun toString() = address
}

// 使用例
when (val result = Email.create("user@example.com")) {
    is EmailResult.Valid -> println("Valid: ${result.email}")
    is EmailResult.Invalid -> println("Invalid: ${result.reason}")
}
```

### オブジェクト式（匿名オブジェクト）

```kotlin
// 匿名オブジェクト
val clickListener = object : MouseListener {
    override fun mouseClicked(event: MouseEvent) {
        println("Mouse clicked at ${event.x}, ${event.y}")
    }

    override fun mouseEntered(event: MouseEvent) {
        println("Mouse entered")
    }
}

// 複数のインターフェースを実装
val multiListener = object : MouseListener, KeyListener {
    override fun mouseClicked(event: MouseEvent) {}
    override fun keyPressed(event: KeyEvent) {}
}

// ローカルシングルトン
fun processData() {
    val cache = object {
        private val data = mutableMapOf<String, Any>()

        fun get(key: String) = data[key]
        fun put(key: String, value: Any) {
            data[key] = value
        }
    }

    cache.put("key", "value")
}
```

**Javaとの比較：**

```java
// Java - 匿名クラス
MouseListener clickListener = new MouseListener() {
    @Override
    public void mouseClicked(MouseEvent event) {
        System.out.println("Mouse clicked at " +
            event.getX() + ", " + event.getY());
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        System.out.println("Mouse entered");
    }
};
```

## 実践的なサンプル

### 実務的なクラス設計

```kotlin
// ドメインモデルの例
data class Order(
    val id: Long,
    val customerId: Long,
    val items: List<OrderItem>,
    val status: OrderStatus,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalAmount: Double
        get() = items.sumOf { it.price * it.quantity }

    fun canBeCancelled(): Boolean = status == OrderStatus.PENDING

    fun cancel(): Order = copy(status = OrderStatus.CANCELLED)

    companion object {
        fun create(customerId: Long, items: List<OrderItem>): Order {
            require(items.isNotEmpty()) { "Order must contain at least one item" }
            return Order(
                id = generateId(),
                customerId = customerId,
                items = items,
                status = OrderStatus.PENDING
            )
        }

        private fun generateId(): Long = System.currentTimeMillis()
    }
}

data class OrderItem(
    val productId: Long,
    val productName: String,
    val price: Double,
    val quantity: Int
)

enum class OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

// サービスクラス
class OrderService(
    private val orderRepository: OrderRepository,
    private val notificationService: NotificationService
) {
    fun placeOrder(customerId: Long, items: List<OrderItem>): Result<Order> {
        return try {
            val order = Order.create(customerId, items)
            val saved = orderRepository.save(order)
            notificationService.sendOrderConfirmation(saved)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cancelOrder(orderId: Long): Result<Order> {
        val order = orderRepository.findById(orderId)
            ?: return Result.failure(OrderNotFoundException(orderId))

        if (!order.canBeCancelled()) {
            return Result.failure(IllegalStateException("Order cannot be cancelled"))
        }

        val cancelled = order.cancel()
        orderRepository.save(cancelled)
        return Result.success(cancelled)
    }
}

// リポジトリインターフェース
interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: Long): Order?
    fun findByCustomerId(customerId: Long): List<Order>
}

class OrderNotFoundException(orderId: Long) :
    Exception("Order not found: $orderId")
```

## まとめ

本章では、Kotlinのオブジェクト指向プログラミングの特徴について学びました：

✅ **プライマリコンストラクタによる簡潔なクラス定義**

✅ **プロパティの自動getter/setter生成とカスタマイズ**

✅ **open/finalによる明示的な継承制御**

✅ **データクラスによるボイラープレートの削減**

✅ **シールドクラスによる型安全な階層表現**

✅ **オブジェクト宣言とコンパニオンオブジェクト**

これらの機能により、Kotlinではより安全で保守しやすいオブジェクト指向コードを書くことができます。

## 演習問題

1. 以下のJavaクラスをKotlinのデータクラスに変換してください：

```java
public class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final double price;

    public Book(String isbn, String title, String author, double price) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.price = price;
    }

    // getter, equals, hashCode, toString...
}
```

2. シールドクラスを使って、計算機の演算結果を表現する型を作成してください。成功（結果の値）、エラー（エラーメッセージ）、ゼロ除算エラーの3つのケースを表現します。

3. シングルトンパターンを使って、アプリケーション全体で共有されるロガークラスを実装してください。ログレベル（INFO, WARNING, ERROR）をサポートし、各ログレベルごとにメッセージを記録できるようにします。
