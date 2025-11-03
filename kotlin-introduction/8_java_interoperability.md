# 第8章：Javaとの相互運用

## 8.1 JavaコードからKotlinを呼び出す

### @JvmStaticと@JvmField

Kotlinのコンパニオンオブジェクトのメンバーを、Javaから静的メンバーとしてアクセスできるようにします。

```kotlin
// Kotlin側の定義
class UserManager {
    companion object {
        // @JvmStaticなし
        fun create(): UserManager {
            return UserManager()
        }

        // @JvmStatic付き
        @JvmStatic
        fun createStatic(): UserManager {
            return UserManager()
        }

        const val MAX_USERS = 1000  // コンパイル時定数

        @JvmField
        val DEFAULT_TIMEOUT = 5000  // Javaから直接アクセス可能
    }
}

// Javaからの呼び出し
public class JavaCaller {
    public void callKotlin() {
        // @JvmStaticなしの場合
        UserManager manager1 = UserManager.Companion.create();

        // @JvmStatic付きの場合
        UserManager manager2 = UserManager.createStatic();

        // 定数のアクセス
        int maxUsers = UserManager.MAX_USERS;

        // @JvmFieldでアクセス
        int timeout = UserManager.DEFAULT_TIMEOUT;
    }
}
```

### オブジェクト宣言とJava

```kotlin
// Kotlinのオブジェクト宣言
object DatabaseConfig {
    val url = "jdbc:postgresql://localhost/mydb"
    val username = "user"

    @JvmStatic
    fun connect() {
        println("Connecting to $url")
    }

    fun disconnect() {
        println("Disconnecting")
    }
}

// Javaからの呼び出し
public class JavaCaller {
    public void useDatabaseConfig() {
        // オブジェクトへのアクセス
        DatabaseConfig instance = DatabaseConfig.INSTANCE;
        String url = instance.getUrl();

        // @JvmStatic付きメソッド
        DatabaseConfig.connect();

        // 通常のメソッド
        DatabaseConfig.INSTANCE.disconnect();
    }
}
```

### @JvmOverloads

デフォルト引数を持つKotlin関数を、Javaから複数のオーバーロードとして呼び出せるようにします。

```kotlin
// Kotlin側の定義
class User {
    @JvmOverloads
    fun greet(
        name: String,
        title: String = "Mr.",
        formal: Boolean = true
    ): String {
        val greeting = if (formal) "Good morning" else "Hi"
        return "$greeting, $title $name"
    }
}

// 生成されるJavaメソッド（概念的に）
// String greet(String name)
// String greet(String name, String title)
// String greet(String name, String title, boolean formal)

// Javaからの呼び出し
public class JavaCaller {
    public void callGreet() {
        User user = new User();

        // 3つのオーバーロードが利用可能
        String greeting1 = user.greet("Alice");
        String greeting2 = user.greet("Alice", "Ms.");
        String greeting3 = user.greet("Alice", "Ms.", false);
    }
}

// コンストラクタでの使用
class Config @JvmOverloads constructor(
    val host: String = "localhost",
    val port: Int = 8080,
    val secure: Boolean = false
)

// Javaから
// Config config1 = new Config();
// Config config2 = new Config("example.com");
// Config config3 = new Config("example.com", 443);
// Config config4 = new Config("example.com", 443, true);
```

### @Throws

Kotlinには検査例外の概念がありませんが、Javaとの互換性のために例外を宣言できます。

```kotlin
// Kotlin側の定義
class FileManager {
    @Throws(IOException::class)
    fun readFile(path: String): String {
        // ファイル読み込み処理
        throw IOException("File not found")
    }

    @Throws(SQLException::class, IOException::class)
    fun saveToDatabase(data: String) {
        // データベース処理
    }

    // @Throwsなし
    fun processData(data: String) {
        throw IllegalArgumentException("Invalid data")
    }
}

// Javaからの呼び出し
public class JavaCaller {
    public void useFileManager() {
        FileManager manager = new FileManager();

        // @Throws付きは例外処理が必要
        try {
            String content = manager.readFile("data.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // @Throwsなしは例外処理不要（コンパイラが強制しない）
        manager.processData("test");
    }
}
```

### @JvmName

Kotlinのメンバー名をJavaから見たときの名前を変更します。

```kotlin
// 拡張関数の名前衝突を回避
@file:JvmName("StringUtils")
package com.example.utils

fun String.capitalize(): String {
    return this.replaceFirstChar { it.uppercase() }
}

fun String.truncate(maxLength: Int): String {
    return if (length > maxLength) {
        substring(0, maxLength) + "..."
    } else {
        this
    }
}

// Javaからの呼び出し
// import com.example.utils.StringUtils;
// String result = StringUtils.capitalize("hello");
// String truncated = StringUtils.truncate("long text", 10);

// getter/setterの名前変更
class User {
    @get:JvmName("getName")
    @set:JvmName("setName")
    var name: String = ""

    // boolean型のプロパティ
    @get:JvmName("isActive")
    var active: Boolean = true
}

// プロパティのアクセサに異なる可視性を設定
class Account {
    var balance: Double = 0.0
        @JvmName("getBalance") get
        @JvmName("updateBalance") private set
}
```

### プロパティとgetter/setter

```kotlin
// Kotlinのプロパティ
class Person(
    val name: String,  // 読み取り専用
    var age: Int       // 読み書き可能
) {
    var email: String = ""
        private set  // 外部からは読み取りのみ

    fun updateEmail(newEmail: String) {
        email = newEmail
    }
}

// Javaからの呼び出し
public class JavaCaller {
    public void usePerson() {
        Person person = new Person("Alice", 25);

        // getter
        String name = person.getName();
        int age = person.getAge();
        String email = person.getEmail();

        // setter（varプロパティのみ）
        person.setAge(26);

        // private setterは使用不可
        // person.setEmail("..."); // コンパイルエラー

        // メソッドを使用
        person.updateEmail("alice@example.com");
    }
}

// カスタムgetter/setter
class Temperature {
    var celsius: Double = 0.0
        set(value) {
            if (value < -273.15) {
                throw IllegalArgumentException("Below absolute zero")
            }
            field = value
        }

    val fahrenheit: Double
        get() = celsius * 9/5 + 32
}

// Javaから
// Temperature temp = new Temperature();
// temp.setCelsius(25.0);
// double f = temp.getFahrenheit();
```

## 8.2 KotlinからJavaコードを使用する

### SAM変換

Single Abstract Method（SAM）インターフェースは、Kotlinのラムダで簡潔に実装できます。

```kotlin
// Javaの関数型インターフェース
@FunctionalInterface
public interface ClickListener {
    void onClick(View view);
}

public interface EventHandler {
    void handleEvent(String event);
}

// Kotlinからの使用
class Button {
    // Javaスタイル（冗長）
    fun setClickListener1(listener: ClickListener) {
        listener.onClick(View())
    }

    // SAM変換を使用（簡潔）
    fun setClickListener2() {
        val button = JavaButton()

        // ラムダで直接実装
        button.setOnClickListener { view ->
            println("Button clicked")
        }

        // 通常の関数参照
        button.setOnClickListener(::handleClick)

        // メソッド参照
        button.setOnClickListener(this::onButtonClick)
    }

    private fun handleClick(view: View) {
        println("Handle click")
    }

    private fun onButtonClick(view: View) {
        println("Button clicked")
    }
}

// より実践的な例
fun processFiles() {
    val files = File(".").listFiles { file ->
        file.isFile && file.extension == "txt"
    }

    // Comparator の SAM変換
    val sorted = files?.sortedWith { f1, f2 ->
        f1.name.compareTo(f2.name)
    }

    // Runnable の SAM変換
    Thread {
        println("Running in thread")
    }.start()

    // Callable の SAM変換
    val executor = Executors.newSingleThreadExecutor()
    val future = executor.submit<String> {
        "Result"
    }
}
```

### Javaのgetter/setterへのアクセス

```kotlin
// Javaクラス
public class JavaPerson {
    private String name;
    private int age;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public boolean isActive() { return true; }
}

// Kotlinからのアクセス
fun useJavaPerson() {
    val person = JavaPerson()

    // プロパティ構文でアクセス可能
    person.name = "Alice"
    person.age = 25

    // getter/setterも直接呼び出せる
    person.setName("Bob")
    val name = person.getName()

    // is...() は boolean プロパティとしてアクセス
    val active = person.isActive  // person.isActive() と同じ
}

// Builderパターンとの連携
fun useBuilder() {
    // Javaのビルダーパターン
    val config = JavaConfig.builder()
        .setHost("localhost")
        .setPort(8080)
        .setSecure(true)
        .build()

    // Kotlinのapplyを使用
    val config2 = JavaConfig.builder().apply {
        host = "localhost"  // setHost("localhost") と同じ
        port = 8080
        secure = true
    }.build()
}
```

### Null安全性の考慮

Kotlinから Java を呼び出す際、nullability の扱いに注意が必要です。

```kotlin
// Javaコード（アノテーションなし）
public class JavaService {
    public String getName() {
        return null;  // nullを返す可能性
    }

    public void processData(String data) {
        // dataがnullかもしれない
    }
}

// Kotlinから使用（プラットフォーム型）
fun useJavaService() {
    val service = JavaService()

    // プラットフォーム型 String!
    val name = service.name  // nullかもしれないが、型システムは非nullとして扱う

    // 安全な扱い方
    val safeName: String? = service.name  // nullable型として扱う
    safeName?.let { println(it) }

    // 非nullとして扱う（NPEのリスク）
    val unsafeName: String = service.name  // nullならNPE
}

// Javaアノテーションの活用
// @Nullable と @NonNull を使用したJavaコード
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnnotatedJavaService {
    @NotNull
    public String getRequiredName() {
        return "Name";
    }

    @Nullable
    public String getOptionalName() {
        return null;
    }

    public void processData(@NotNull String data) {
        // dataは非null
    }
}

// Kotlinからの使用
fun useAnnotatedService() {
    val service = AnnotatedJavaService()

    // @NotNull -> String型
    val required: String = service.requiredName  // OK

    // @Nullable -> String?型
    val optional: String? = service.optionalName  // OK

    // @NotNullパラメータ
    service.processData("data")  // OK
    // service.processData(null)  // コンパイルエラー
}

// Java 8 Optional との連携
fun useOptional() {
    val javaService = JavaOptionalService()

    // OptionalをKotlinのnullableに変換
    val value: String? = javaService.findValue().orElse(null)

    // 拡張関数での変換
    fun <T> java.util.Optional<T>.toNullable(): T? = orElse(null)

    val value2: String? = javaService.findValue().toNullable()
}
```

### プラットフォーム型の扱い

```kotlin
// プラットフォーム型（T!）とは
// Javaから来た型で、nullableかnon-nullableか不明な型

public class JavaApi {
    public String getData() {
        // @Nullable/@NonNull アノテーションなし
        return someData;
    }

    public List<String> getItems() {
        return items;
    }
}

// Kotlinでの扱い方
fun handlePlatformTypes() {
    val api = JavaApi()

    // 方法1: nullable型として扱う（安全）
    val data1: String? = api.data
    data1?.let { println(it) }

    // 方法2: 非null型として扱う（危険）
    val data2: String = api.data  // nullならNPE

    // 方法3: 即座にnullチェック
    val data3 = requireNotNull(api.data) { "Data must not be null" }

    // コレクションの場合
    val items1: List<String>? = api.items  // リストがnull
    val items2: List<String?>? = api.items  // リストまたは要素がnull
    val items3: List<String?> = api.items  // 要素がnull

    // 安全な処理
    val safeItems = api.items?.filterNotNull() ?: emptyList()
}

// ラッパークラスで安全性を提供
class SafeJavaApiWrapper(private val api: JavaApi) {
    fun getData(): String? = api.data

    fun getItems(): List<String> = api.items?.filterNotNull() ?: emptyList()

    fun requireData(): String = requireNotNull(api.data) {
        "Data is required but was null"
    }
}
```

### Javaコレクションの扱い

```kotlin
// Javaコレクション
public class JavaCollections {
    public List<String> getNames() {
        return Arrays.asList("Alice", "Bob", "Charlie");
    }

    public Map<String, Integer> getScores() {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 90);
        scores.put("Bob", 85);
        return scores;
    }
}

// Kotlinからの使用
fun useJavaCollections() {
    val collections = JavaCollections()

    // Javaリストは可変リストとして扱われる
    val names = collections.names  // MutableList<String!>!
    names.add("David")  // 可能だが推奨されない

    // 安全な使い方：イミュータブルにコピー
    val safeNames: List<String> = collections.names
        .filterNotNull()
        .toList()

    // マップの処理
    val scores = collections.scores
    val safeScores: Map<String, Int> = scores
        .filterKeys { it != null }
        .filterValues { it != null }
        .mapValues { it.value!! }

    // より実践的な例
    fun processList(javaList: java.util.List<String>?) {
        // nullと空のチェック
        if (javaList.isNullOrEmpty()) {
            return
        }

        // null要素の除去
        val cleanList = javaList.filterNotNull()

        // Kotlinコレクションとして処理
        cleanList
            .filter { it.length > 3 }
            .map { it.uppercase() }
            .forEach { println(it) }
    }
}
```

## 8.3 段階的な移行戦略

### 既存JavaプロジェクトへのKotlin導入

```kotlin
// ステップ1: Kotlinサポートの追加
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.0"
}

dependencies {
    implementation(kotlin("stdlib"))
}

// ステップ2: 新しいユーティリティクラスをKotlinで作成
// StringExtensions.kt
package com.example.utils

fun String.truncate(maxLength: Int): String {
    return if (length > maxLength) {
        substring(0, maxLength) + "..."
    } else {
        this
    }
}

fun String.isValidEmail(): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    return matches(emailRegex)
}

// Javaから使用
public class JavaCode {
    public void useKotlinExtensions() {
        String text = "Very long text...";
        String truncated = StringExtensionsKt.truncate(text, 10);

        String email = "test@example.com";
        boolean valid = StringExtensionsKt.isValidEmail(email);
    }
}

// ステップ3: 新機能をKotlinで実装
class UserService(
    private val userRepository: JavaUserRepository,
    private val emailService: JavaEmailService
) {
    suspend fun registerUser(request: RegistrationRequest): Result<User> {
        return runCatching {
            // Javaのリポジトリを使用
            val user = userRepository.createUser(
                request.email,
                request.name
            )

            // Javaのサービスを使用
            emailService.sendWelcomeEmail(user.getEmail())

            user
        }
    }
}
```

### テストコードから始める移行

```kotlin
// 既存のJavaクラス
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return a / b;
    }
}

// Kotlinテストコード
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CalculatorTest {
    private val calculator = Calculator()

    @Test
    fun `add should return sum of two numbers`() {
        val result = calculator.add(2, 3)
        assertEquals(5, result)
    }

    @Test
    fun `divide should throw exception for zero divisor`() {
        assertThrows<ArithmeticException> {
            calculator.divide(10.0, 0.0)
        }
    }

    @Test
    fun `divide should return correct result`() {
        val result = calculator.divide(10.0, 2.0)
        assertEquals(5.0, result, 0.001)
    }
}

// より高度なテスト例（MockK使用）
import io.mockk.*
import kotlinx.coroutines.test.runTest

class UserServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val emailService = mockk<EmailService>()
    private val userService = UserService(userRepository, emailService)

    @Test
    fun `should register user successfully`() = runTest {
        // Given
        val request = RegistrationRequest("test@example.com", "Test User")
        val expectedUser = User(1, "test@example.com", "Test User")

        every { userRepository.createUser(any(), any()) } returns expectedUser
        every { emailService.sendWelcomeEmail(any()) } just Runs

        // When
        val result = userService.registerUser(request)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
        verify { emailService.sendWelcomeEmail("test@example.com") }
    }
}
```

### リファクタリングのベストプラクティス

```kotlin
// Before (Java)
public class OrderProcessor {
    private OrderRepository repository;
    private PaymentService paymentService;
    private EmailService emailService;

    public OrderProcessor(OrderRepository repository,
                         PaymentService paymentService,
                         EmailService emailService) {
        this.repository = repository;
        this.paymentService = paymentService;
        this.emailService = emailService;
    }

    public Order processOrder(OrderRequest request) throws ProcessingException {
        try {
            // Validate
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new IllegalArgumentException("Order must have items");
            }

            // Create order
            Order order = new Order();
            order.setCustomerId(request.getCustomerId());
            order.setItems(request.getItems());
            order.setStatus(OrderStatus.PENDING);

            // Save
            Order savedOrder = repository.save(order);

            // Process payment
            PaymentResult payment = paymentService.processPayment(
                savedOrder.getId(),
                savedOrder.getTotalAmount()
            );

            if (payment.isSuccessful()) {
                savedOrder.setStatus(OrderStatus.CONFIRMED);
                savedOrder.setPaymentId(payment.getPaymentId());
                repository.update(savedOrder);

                // Send confirmation
                emailService.sendOrderConfirmation(savedOrder);

                return savedOrder;
            } else {
                savedOrder.setStatus(OrderStatus.FAILED);
                repository.update(savedOrder);
                throw new ProcessingException("Payment failed");
            }
        } catch (Exception e) {
            throw new ProcessingException("Failed to process order", e);
        }
    }
}

// After (Kotlin) - 段階的リファクタリング
class OrderProcessor(
    private val repository: OrderRepository,
    private val paymentService: PaymentService,
    private val emailService: EmailService
) {
    suspend fun processOrder(request: OrderRequest): Result<Order> = runCatching {
        // Validate
        require(request.items.isNotEmpty()) { "Order must have items" }

        // Create order
        val order = Order(
            customerId = request.customerId,
            items = request.items,
            status = OrderStatus.PENDING
        )

        // Save
        val savedOrder = repository.save(order)

        // Process payment
        val payment = paymentService.processPayment(
            orderId = savedOrder.id,
            amount = savedOrder.totalAmount
        )

        // Update status
        when (payment) {
            is PaymentResult.Success -> {
                savedOrder.copy(
                    status = OrderStatus.CONFIRMED,
                    paymentId = payment.paymentId
                ).also { updated ->
                    repository.update(updated)
                    emailService.sendOrderConfirmation(updated)
                }
            }
            is PaymentResult.Failure -> {
                savedOrder.copy(status = OrderStatus.FAILED)
                    .also { repository.update(it) }
                throw ProcessingException("Payment failed: ${payment.reason}")
            }
        }
    }
}

// 移行のポイント
// 1. コンストラクタ注入の簡略化
// 2. データクラスの活用
// 3. null安全性の導入
// 4. suspend関数での非同期処理
// 5. Result型によるエラーハンドリング
// 6. when式による分岐の明確化
// 7. copyメソッドによるイミュータブルな更新

// 混在環境でのベストプラクティス
// 1. パッケージ構造を保つ
// com.example.service (Java)
// com.example.service (Kotlin)

// 2. インターフェースは共通化
interface UserRepository {
    fun findById(id: Long): User?
    fun save(user: User): User
}

// Java実装
public class JdbcUserRepository implements UserRepository {
    @Override
    public User findById(Long id) { /* ... */ }

    @Override
    public User save(User user) { /* ... */ }
}

// Kotlin実装
class KotlinUserRepository : UserRepository {
    override fun findById(id: Long): User? { /* ... */ }
    override fun save(user: User): User { /* ... */ }
}

// 3. 徐々に移行
// Phase 1: 新規コードをKotlinで
// Phase 2: テストをKotlinに移行
// Phase 3: ユーティリティクラスを移行
// Phase 4: サービス層を移行
// Phase 5: 残りのコアロジックを移行
```

## まとめ

本章では、KotlinとJavaの相互運用について学びました：

✅ **@JvmStatic, @JvmField, @JvmOverloadsによるJavaからのアクセス改善**
✅ **SAM変換によるJava関数型インターフェースの簡潔な実装**
✅ **プラットフォーム型の理解とNull安全な扱い方**
✅ **段階的な移行戦略とベストプラクティス**
✅ **既存Javaコードベースへの安全な導入方法**

KotlinとJavaのシームレスな相互運用により、既存プロジェクトへの段階的な導入が可能です。

## 演習問題

1. JavaのBuilderパターンを使用するクラスを、Kotlinから使いやすくするラッパークラスを作成してください。apply関数を活用して簡潔に記述できるようにします。

2. null許容性が不明なJava APIをラップして、Kotlinから安全に使用できるファサードクラスを実装してください。すべての戻り値を適切にnull処理します。

3. 既存のJavaサービスクラス（複数のメソッド、例外処理、同期処理を含む）を、Kotlinのコルーチンとsuspend関数を使って非同期化するリファクタリングを実施してください。