# 第10章：実践演習 - Javaコードのリファクタリング

## 10.1 典型的なJavaパターンのKotlin化

### Builderパターン

Javaのビルダーパターンは、Kotlinのデフォルト引数と名前付き引数で簡潔に置き換えられます。

#### 演習1: ビルダーパターンのリファクタリング

**Java版（Before）:**

```java
// Javaのビルダーパターン
public class HttpClient {
    private final String baseUrl;
    private final int timeout;
    private final int maxRetries;
    private final boolean followRedirects;
    private final Map<String, String> headers;

    private HttpClient(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
        this.followRedirects = builder.followRedirects;
        this.headers = new HashMap<>(builder.headers);
    }

    public static class Builder {
        private String baseUrl;
        private int timeout = 30000;
        private int maxRetries = 3;
        private boolean followRedirects = true;
        private Map<String, String> headers = new HashMap<>();

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public Builder addHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public HttpClient build() {
            if (baseUrl == null) {
                throw new IllegalStateException("baseUrl is required");
            }
            return new HttpClient(this);
        }
    }
}

// 使用例
HttpClient client = new HttpClient.Builder()
    .baseUrl("https://api.example.com")
    .timeout(60000)
    .maxRetries(5)
    .addHeader("Authorization", "Bearer token")
    .build();
```

**Kotlin版（After）:**

```kotlin
// Kotlinのデータクラスとデフォルト引数
data class HttpClient(
    val baseUrl: String,
    val timeout: Int = 30000,
    val maxRetries: Int = 3,
    val followRedirects: Boolean = true,
    val headers: Map<String, String> = emptyMap()
) {
    init {
        require(baseUrl.isNotBlank()) { "baseUrl is required" }
        require(timeout > 0) { "timeout must be positive" }
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
    }

    // 追加のヘルパーメソッド
    fun withHeader(key: String, value: String): HttpClient {
        return copy(headers = headers + (key to value))
    }

    fun withHeaders(newHeaders: Map<String, String>): HttpClient {
        return copy(headers = headers + newHeaders)
    }
}

// 使用例1: シンプルなコンストラクタ呼び出し
val client1 = HttpClient(
    baseUrl = "https://api.example.com",
    timeout = 60000,
    maxRetries = 5,
    headers = mapOf("Authorization" to "Bearer token")
)

// 使用例2: applyを使用
val client2 = HttpClient("https://api.example.com").apply {
    // 必要に応じて追加の設定
}

// 使用例3: copyを使った変更
val client3 = client1.copy(timeout = 90000)

// 使用例4: チェーン可能なヘルパーメソッド
val client4 = HttpClient("https://api.example.com")
    .withHeader("Authorization", "Bearer token")
    .withHeader("Content-Type", "application/json")
```

**練習問題：**
以下のJavaビルダーパターンをKotlinに変換してください：

```java
public class DatabaseConfig {
    public static class Builder {
        private String host = "localhost";
        private int port = 5432;
        private String database;
        private String username;
        private String password;
        private int maxConnections = 10;
        private boolean useSSL = false;

        // setter methods...
        public DatabaseConfig build() { /* ... */ }
    }
}
```

### Factoryパターン

#### 演習2: ファクトリーパターンのリファクタリング

**Java版（Before）:**

```java
// Javaのファクトリーパターン
public abstract class Shape {
    public abstract double area();
    public abstract String describe();
}

public class Circle extends Shape {
    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public String describe() {
        return "Circle with radius " + radius;
    }
}

public class Rectangle extends Shape {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double area() {
        return width * height;
    }

    @Override
    public String describe() {
        return "Rectangle " + width + "x" + height;
    }
}

public class ShapeFactory {
    public static Shape createShape(String type, double... params) {
        switch (type.toLowerCase()) {
            case "circle":
                if (params.length != 1) {
                    throw new IllegalArgumentException("Circle requires 1 parameter");
                }
                return new Circle(params[0]);
            case "rectangle":
                if (params.length != 2) {
                    throw new IllegalArgumentException("Rectangle requires 2 parameters");
                }
                return new Rectangle(params[0], params[1]);
            default:
                throw new IllegalArgumentException("Unknown shape type: " + type);
        }
    }
}

// 使用例
Shape circle = ShapeFactory.createShape("circle", 5.0);
Shape rectangle = ShapeFactory.createShape("rectangle", 4.0, 6.0);
```

**Kotlin版（After）:**

```kotlin
// Kotlinのシールドクラスとコンパニオンオブジェクト
sealed class Shape {
    abstract fun area(): Double
    abstract fun describe(): String

    data class Circle(val radius: Double) : Shape() {
        override fun area(): Double = Math.PI * radius * radius
        override fun describe(): String = "Circle with radius $radius"
    }

    data class Rectangle(val width: Double, val height: Double) : Shape() {
        override fun area(): Double = width * height
        override fun describe(): String = "Rectangle ${width}x${height}"
    }

    data class Triangle(val base: Double, val height: Double) : Shape() {
        override fun area(): Double = base * height / 2
        override fun describe(): String = "Triangle with base $base and height $height"
    }

    companion object {
        fun create(type: String, vararg params: Double): Shape {
            return when (type.lowercase()) {
                "circle" -> {
                    require(params.size == 1) { "Circle requires 1 parameter" }
                    Circle(params[0])
                }
                "rectangle" -> {
                    require(params.size == 2) { "Rectangle requires 2 parameters" }
                    Rectangle(params[0], params[1])
                }
                "triangle" -> {
                    require(params.size == 2) { "Triangle requires 2 parameters" }
                    Triangle(params[0], params[1])
                }
                else -> error("Unknown shape type: $type")
            }
        }
    }
}

// より型安全なアプローチ
sealed class ShapeConfig {
    data class CircleConfig(val radius: Double) : ShapeConfig()
    data class RectangleConfig(val width: Double, val height: Double) : ShapeConfig()
    data class TriangleConfig(val base: Double, val height: Double) : ShapeConfig()
}

fun createShape(config: ShapeConfig): Shape = when (config) {
    is ShapeConfig.CircleConfig -> Shape.Circle(config.radius)
    is ShapeConfig.RectangleConfig -> Shape.Rectangle(config.width, config.height)
    is ShapeConfig.TriangleConfig -> Shape.Triangle(config.base, config.height)
}

// 使用例
val circle = Shape.create("circle", 5.0)
val rectangle = Shape.create("rectangle", 4.0, 6.0)

// 型安全な方法
val circle2 = createShape(ShapeConfig.CircleConfig(5.0))
val rectangle2 = createShape(ShapeConfig.RectangleConfig(4.0, 6.0))

// when式での網羅的チェック
fun processShape(shape: Shape) {
    when (shape) {
        is Shape.Circle -> println("Processing circle: ${shape.radius}")
        is Shape.Rectangle -> println("Processing rectangle: ${shape.width}x${shape.height}")
        is Shape.Triangle -> println("Processing triangle: ${shape.base}x${shape.height}")
        // 新しい型を追加するとコンパイルエラーになる（elseが必要になる）
    }
}
```

### Strategyパターン

#### 演習3: ストラテジーパターンのリファクタリング

**Java版（Before）:**

```java
// Javaのストラテジーパターン
public interface PaymentStrategy {
    boolean pay(double amount);
}

public class CreditCardStrategy implements PaymentStrategy {
    private String cardNumber;
    private String cvv;

    public CreditCardStrategy(String cardNumber, String cvv) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("Paid " + amount + " using Credit Card");
        return true;
    }
}

public class PayPalStrategy implements PaymentStrategy {
    private String email;
    private String password;

    public PayPalStrategy(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("Paid " + amount + " using PayPal");
        return true;
    }
}

public class ShoppingCart {
    private PaymentStrategy paymentStrategy;

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public void checkout(double amount) {
        if (paymentStrategy == null) {
            throw new IllegalStateException("Payment strategy not set");
        }
        paymentStrategy.pay(amount);
    }
}

// 使用例
ShoppingCart cart = new ShoppingCart();
cart.setPaymentStrategy(new CreditCardStrategy("1234-5678", "123"));
cart.checkout(100.0);

cart.setPaymentStrategy(new PayPalStrategy("user@example.com", "password"));
cart.checkout(50.0);
```

**Kotlin版（After）:**

```kotlin
// Kotlinの関数型アプローチ
sealed class PaymentMethod {
    data class CreditCard(val cardNumber: String, val cvv: String) : PaymentMethod()
    data class PayPal(val email: String) : PaymentMethod()
    data class BankTransfer(val accountNumber: String, val bankCode: String) : PaymentMethod()
    object Cash : PaymentMethod()
}

sealed class PaymentResult {
    data class Success(val transactionId: String) : PaymentResult()
    data class Failure(val reason: String) : PaymentResult()
}

// 関数型のアプローチ
typealias PaymentProcessor = (Double, PaymentMethod) -> PaymentResult

fun processPayment(amount: Double, method: PaymentMethod): PaymentResult {
    return when (method) {
        is PaymentMethod.CreditCard -> {
            println("Processing credit card payment: $amount")
            PaymentResult.Success("CC-${System.currentTimeMillis()}")
        }
        is PaymentMethod.PayPal -> {
            println("Processing PayPal payment: $amount")
            PaymentResult.Success("PP-${System.currentTimeMillis()}")
        }
        is PaymentMethod.BankTransfer -> {
            println("Processing bank transfer: $amount")
            PaymentResult.Success("BT-${System.currentTimeMillis()}")
        }
        PaymentMethod.Cash -> {
            println("Processing cash payment: $amount")
            PaymentResult.Success("CASH-${System.currentTimeMillis()}")
        }
    }
}

// より高度な例：ストラテジーをクラスとして実装
class ShoppingCart(private val processor: PaymentProcessor = ::processPayment) {
    private val items = mutableListOf<CartItem>()

    fun addItem(item: CartItem) {
        items.add(item)
    }

    fun checkout(method: PaymentMethod): PaymentResult {
        val total = items.sumOf { it.price * it.quantity }
        require(total > 0) { "Cart is empty" }

        return processor(total, method)
    }

    fun total(): Double = items.sumOf { it.price * it.quantity }
}

data class CartItem(val name: String, val price: Double, val quantity: Int)

// 使用例1: シンプルな使い方
val cart1 = ShoppingCart()
cart1.addItem(CartItem("Book", 20.0, 2))
cart1.addItem(CartItem("Pen", 1.5, 5))

val result1 = cart1.checkout(PaymentMethod.CreditCard("1234-5678", "123"))
when (result1) {
    is PaymentResult.Success -> println("Payment successful: ${result1.transactionId}")
    is PaymentResult.Failure -> println("Payment failed: ${result1.reason}")
}

// 使用例2: カスタムプロセッサ
val customProcessor: PaymentProcessor = { amount, method ->
    // カスタムロジック
    PaymentResult.Success("CUSTOM-${System.currentTimeMillis()}")
}

val cart2 = ShoppingCart(customProcessor)
cart2.addItem(CartItem("Laptop", 1000.0, 1))
cart2.checkout(PaymentMethod.PayPal("user@example.com"))

// 使用例3: ラムダでの簡潔な実装
val cart3 = ShoppingCart { amount, method ->
    println("Custom payment: $amount with $method")
    PaymentResult.Success("TX-${System.currentTimeMillis()}")
}
```

**練習問題：**
以下のJavaストラテジーパターンをKotlinに変換してください：

```java
// ソート戦略のインターフェース
public interface SortStrategy {
    <T extends Comparable<T>> void sort(List<T> list);
}

public class QuickSortStrategy implements SortStrategy { /* ... */ }
public class MergeSortStrategy implements SortStrategy { /* ... */ }
public class BubbleSortStrategy implements SortStrategy { /* ... */ }

public class Sorter {
    private SortStrategy strategy;
    public void setStrategy(SortStrategy strategy) { /* ... */ }
    public <T extends Comparable<T>> void sort(List<T> list) { /* ... */ }
}
```

## 10.2 レガシーコードの改善

### ボイラープレートの削減

#### 演習4: POJOクラスのリファクタリング

**Java版（Before）:**

```java
// 典型的なJava POJO
public class User {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Integer age;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    public User(Long id, String username, String email, String firstName,
                String lastName, Integer age, boolean active,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Integer getAge() { return age; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setAge(Integer age) { this.age = age; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return active == user.active &&
               Objects.equals(id, user.id) &&
               Objects.equals(username, user.username) &&
               Objects.equals(email, user.email) &&
               Objects.equals(firstName, user.firstName) &&
               Objects.equals(lastName, user.lastName) &&
               Objects.equals(age, user.age) &&
               Objects.equals(createdAt, user.createdAt) &&
               Objects.equals(updatedAt, user.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email, firstName, lastName,
                          age, active, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "User{" +
               "id=" + id +
               ", username='" + username + '\'' +
               ", email='" + email + '\'' +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", age=" + age +
               ", active=" + active +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }

    // ビジネスロジック
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isAdult() {
        return age != null && age >= 18;
    }
}
```

**Kotlin版（After）:**

```kotlin
// Kotlinのデータクラス
data class User(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val age: Int?,
    val active: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // ビジネスロジックのみ記述
    val fullName: String
        get() = "$firstName $lastName"

    val isAdult: Boolean
        get() = age?.let { it >= 18 } ?: false

    // バリデーション
    init {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(email.contains("@")) { "Invalid email format" }
        age?.let { require(it >= 0) { "Age cannot be negative" } }
    }

    // 追加のヘルパーメソッド
    fun activate() = copy(active = true, updatedAt = LocalDateTime.now())
    fun deactivate() = copy(active = false, updatedAt = LocalDateTime.now())
    fun updateEmail(newEmail: String) = copy(email = newEmail, updatedAt = LocalDateTime.now())
}

// 使用例
val user = User(
    id = 1,
    username = "alice",
    email = "alice@example.com",
    firstName = "Alice",
    lastName = "Smith",
    age = 25
)

// 自動生成されるメソッド
println(user)  // toString()
val user2 = user.copy(age = 26)  // copy()
val (id, username) = user  // 分解宣言

// ビジネスロジック
println(user.fullName)  // "Alice Smith"
println(user.isAdult)  // true

// イミュータブルな更新
val activatedUser = user.activate()
val updatedUser = user.updateEmail("newemail@example.com")
```

### 可読性の向上

#### 演習5: 複雑なビジネスロジックのリファクタリング

**Java版（Before）:**

```java
// 複雑で読みにくいJavaコード
public class OrderService {
    public Order processOrder(OrderRequest request) throws OrderException {
        // 検証
        if (request == null) {
            throw new OrderException("Request cannot be null");
        }
        if (request.getCustomerId() == null || request.getCustomerId() <= 0) {
            throw new OrderException("Invalid customer ID");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderException("Order must contain items");
        }

        // 顧客の取得
        Customer customer = customerRepository.findById(request.getCustomerId());
        if (customer == null) {
            throw new OrderException("Customer not found");
        }
        if (!customer.isActive()) {
            throw new OrderException("Customer is not active");
        }

        // 在庫チェック
        for (OrderItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId());
            if (product == null) {
                throw new OrderException("Product not found: " + item.getProductId());
            }
            if (product.getStock() < item.getQuantity()) {
                throw new OrderException("Insufficient stock for product: " + product.getName());
            }
        }

        // 合計金額の計算
        double total = 0.0;
        for (OrderItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId());
            total += product.getPrice() * item.getQuantity();
        }

        // 割引の適用
        if (customer.isPremium()) {
            total *= 0.9;  // 10% discount
        }
        if (total > 100.0) {
            total -= 10.0;  // $10 off for orders over $100
        }

        // 注文の作成
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setItems(request.getItems());
        order.setTotal(total);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        // 在庫の更新
        for (OrderItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId());
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.update(product);
        }

        // 保存
        Order savedOrder = orderRepository.save(order);

        // 通知
        try {
            emailService.sendOrderConfirmation(customer.getEmail(), savedOrder);
        } catch (Exception e) {
            // ログして続行
            logger.error("Failed to send email", e);
        }

        return savedOrder;
    }
}
```

**Kotlin版（After）:**

```kotlin
// 読みやすく構造化されたKotlinコード
class OrderService(
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val emailService: EmailService
) {
    suspend fun processOrder(request: OrderRequest): Result<Order> = runCatching {
        // バリデーション
        val validatedRequest = validateRequest(request)

        // 顧客の取得と検証
        val customer = fetchAndValidateCustomer(validatedRequest.customerId)

        // 在庫チェック
        validateInventory(validatedRequest.items)

        // 金額計算
        val total = calculateTotal(validatedRequest.items, customer)

        // 注文作成
        val order = createOrder(validatedRequest, total)

        // 在庫更新
        updateInventory(validatedRequest.items)

        // 保存
        val savedOrder = orderRepository.save(order)

        // 通知（非同期、失敗しても続行）
        notifyCustomer(customer, savedOrder)

        savedOrder
    }

    private fun validateRequest(request: OrderRequest): OrderRequest {
        require(request.customerId > 0) { "Invalid customer ID" }
        require(request.items.isNotEmpty()) { "Order must contain items" }
        return request
    }

    private suspend fun fetchAndValidateCustomer(customerId: Long): Customer {
        val customer = customerRepository.findById(customerId)
            ?: throw OrderException("Customer not found: $customerId")

        require(customer.isActive) { "Customer is not active" }

        return customer
    }

    private suspend fun validateInventory(items: List<OrderItem>) {
        items.forEach { item ->
            val product = productRepository.findById(item.productId)
                ?: throw OrderException("Product not found: ${item.productId}")

            require(product.stock >= item.quantity) {
                "Insufficient stock for product: ${product.name}"
            }
        }
    }

    private suspend fun calculateTotal(items: List<OrderItem>, customer: Customer): Double {
        val subtotal = items.sumOf { item ->
            val product = productRepository.findById(item.productId)
                ?: throw OrderException("Product not found: ${item.productId}")
            product.price * item.quantity
        }

        return applyDiscounts(subtotal, customer)
    }

    private fun applyDiscounts(amount: Double, customer: Customer): Double {
        var total = amount

        // プレミアム会員割引
        if (customer.isPremium) {
            total *= 0.9
        }

        // 金額割引
        if (total > 100.0) {
            total -= 10.0
        }

        return total
    }

    private fun createOrder(request: OrderRequest, total: Double): Order {
        return Order(
            customerId = request.customerId,
            items = request.items,
            total = total,
            status = OrderStatus.PENDING,
            createdAt = LocalDateTime.now()
        )
    }

    private suspend fun updateInventory(items: List<OrderItem>) {
        items.forEach { item ->
            val product = productRepository.findById(item.productId)
                ?: return@forEach

            val updated = product.copy(stock = product.stock - item.quantity)
            productRepository.update(updated)
        }
    }

    private suspend fun notifyCustomer(customer: Customer, order: Order) {
        runCatching {
            emailService.sendOrderConfirmation(customer.email, order)
        }.onFailure { error ->
            logger.error("Failed to send email", error)
        }
    }
}

// データクラス
data class OrderRequest(
    val customerId: Long,
    val items: List<OrderItem>
)

data class OrderItem(
    val productId: Long,
    val quantity: Int
)

data class Order(
    val id: Long = 0,
    val customerId: Long,
    val items: List<OrderItem>,
    val total: Double,
    val status: OrderStatus,
    val createdAt: LocalDateTime
)

enum class OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

class OrderException(message: String) : Exception(message)
```

### 保守性の改善

#### 演習6: エラーハンドリングの改善

**Java版（Before）:**

```java
// 複雑なエラーハンドリング
public class UserService {
    public User loginUser(String email, String password) {
        try {
            if (email == null || email.isEmpty()) {
                throw new ValidationException("Email is required");
            }
            if (password == null || password.isEmpty()) {
                throw new ValidationException("Password is required");
            }

            User user = userRepository.findByEmail(email);
            if (user == null) {
                throw new AuthenticationException("Invalid credentials");
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new AuthenticationException("Invalid credentials");
            }

            if (!user.isActive()) {
                throw new AccountException("Account is not active");
            }

            user.setLastLogin(LocalDateTime.now());
            userRepository.update(user);

            String token = tokenService.generateToken(user);
            user.setToken(token);

            return user;
        } catch (ValidationException e) {
            logger.warn("Validation error: " + e.getMessage());
            throw e;
        } catch (AuthenticationException e) {
            logger.warn("Authentication error: " + e.getMessage());
            throw e;
        } catch (AccountException e) {
            logger.warn("Account error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during login", e);
            throw new RuntimeException("Login failed", e);
        }
    }
}
```

**Kotlin版（After）:**

```kotlin
// 型安全で明確なエラーハンドリング
sealed class LoginResult {
    data class Success(val user: User, val token: String) : LoginResult()
    sealed class Failure : LoginResult() {
        data class ValidationError(val message: String) : Failure()
        object InvalidCredentials : Failure()
        object AccountInactive : Failure()
        data class SystemError(val error: Throwable) : Failure()
    }
}

class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
    private val logger: Logger
) {
    suspend fun loginUser(email: String, password: String): LoginResult {
        return try {
            // バリデーション
            validateCredentials(email, password)
                .onFailure { return LoginResult.Failure.ValidationError(it) }

            // ユーザー取得
            val user = userRepository.findByEmail(email)
                ?: return LoginResult.Failure.InvalidCredentials

            // パスワード検証
            if (!passwordEncoder.matches(password, user.password)) {
                logger.warn("Invalid password attempt for email: $email")
                return LoginResult.Failure.InvalidCredentials
            }

            // アクティブチェック
            if (!user.isActive) {
                logger.warn("Inactive account login attempt: $email")
                return LoginResult.Failure.AccountInactive
            }

            // 更新
            val updated = user.copy(lastLogin = LocalDateTime.now())
            userRepository.update(updated)

            // トークン生成
            val token = tokenService.generateToken(updated)

            logger.info("Successful login for user: $email")
            LoginResult.Success(updated, token)

        } catch (e: Exception) {
            logger.error("Unexpected error during login", e)
            LoginResult.Failure.SystemError(e)
        }
    }

    private fun validateCredentials(email: String, password: String): Result<Unit> {
        return when {
            email.isBlank() -> Result.failure(Exception("Email is required"))
            !email.contains("@") -> Result.failure(Exception("Invalid email format"))
            password.isBlank() -> Result.failure(Exception("Password is required"))
            password.length < 8 -> Result.failure(Exception("Password too short"))
            else -> Result.success(Unit)
        }
    }
}

// 使用例
suspend fun handleLogin(email: String, password: String) {
    when (val result = userService.loginUser(email, password)) {
        is LoginResult.Success -> {
            println("Welcome ${result.user.name}")
            saveToken(result.token)
            navigateToHome()
        }
        is LoginResult.Failure.ValidationError -> {
            showError("Validation error: ${result.message}")
        }
        LoginResult.Failure.InvalidCredentials -> {
            showError("Invalid email or password")
        }
        LoginResult.Failure.AccountInactive -> {
            showError("Your account is not active. Please contact support.")
        }
        is LoginResult.Failure.SystemError -> {
            showError("System error. Please try again later.")
            reportError(result.error)
        }
    }
}
```

## 10.3 総合演習

### 演習7: 実際のJavaクラスの完全なリファクタリング

**課題：**
以下のJavaコードを、これまで学んだKotlinの機能を使って完全にリファクタリングしてください。

**Java版（Before）:**

```java
// レガシーなJavaコード
public class ReportGenerator {
    private DataSource dataSource;
    private ReportFormatter formatter;
    private EmailSender emailSender;

    public ReportGenerator(DataSource dataSource,
                          ReportFormatter formatter,
                          EmailSender emailSender) {
        this.dataSource = dataSource;
        this.formatter = formatter;
        this.emailSender = emailSender;
    }

    public void generateAndSendReport(ReportRequest request) {
        try {
            // Validate request
            if (request == null) {
                throw new IllegalArgumentException("Request cannot be null");
            }
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new IllegalArgumentException("Date range is required");
            }
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("Invalid date range");
            }

            // Fetch data
            List<DataRecord> records = dataSource.fetchRecords(
                request.getStartDate(),
                request.getEndDate()
            );

            if (records == null || records.isEmpty()) {
                throw new NoDataException("No data available for the specified period");
            }

            // Process data
            Map<String, Double> summary = new HashMap<>();
            double total = 0.0;
            for (DataRecord record : records) {
                String category = record.getCategory();
                double amount = record.getAmount();

                summary.put(category, summary.getOrDefault(category, 0.0) + amount);
                total += amount;
            }

            // Create report
            Report report = new Report();
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setRecordCount(records.size());
            report.setSummary(summary);
            report.setTotal(total);
            report.setGeneratedAt(LocalDateTime.now());

            // Format report
            String formattedReport = formatter.format(report, request.getFormat());

            // Send email
            if (request.getRecipients() != null && !request.getRecipients().isEmpty()) {
                for (String recipient : request.getRecipients()) {
                    try {
                        emailSender.send(
                            recipient,
                            "Report for " + request.getStartDate() + " to " + request.getEndDate(),
                            formattedReport
                        );
                    } catch (EmailException e) {
                        System.err.println("Failed to send email to " + recipient + ": " + e.getMessage());
                    }
                }
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            throw e;
        } catch (NoDataException e) {
            System.err.println("No data: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Report generation failed", e);
        }
    }
}

public class ReportRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String format;
    private List<String> recipients;

    // getters and setters...
}

public class Report {
    private LocalDate startDate;
    private LocalDate endDate;
    private int recordCount;
    private Map<String, Double> summary;
    private double total;
    private LocalDateTime generatedAt;

    // getters and setters...
}
```

**Kotlin版（After）- あなたの実装：**

```kotlin
// ここにあなたのKotlin実装を書いてください
// 以下の機能を含めること：
// 1. データクラスの活用
// 2. sealed class によるエラーハンドリング
// 3. suspend関数による非同期処理
// 4. コルーチンの活用
// 5. null安全性
// 6. 関数型プログラミングスタイル
// 7. 拡張関数
// 8. スコープ関数

// ヒント：
// - ReportRequestとReportをdata classに
// - Result型またはsealed classでエラーハンドリング
// - コレクション操作を関数型スタイルで
// - suspend関数で非同期処理
```

**模範解答例：**

```kotlin
// データクラス
data class ReportRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val format: ReportFormat = ReportFormat.PDF,
    val recipients: List<String> = emptyList()
) {
    init {
        require(startDate <= endDate) { "Invalid date range" }
    }
}

data class Report(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val recordCount: Int,
    val summary: Map<String, Double>,
    val total: Double,
    val generatedAt: LocalDateTime = LocalDateTime.now()
)

enum class ReportFormat { PDF, HTML, CSV }

// Result型
sealed class ReportResult {
    data class Success(val report: Report, val emailsSent: Int) : ReportResult()
    sealed class Failure : ReportResult() {
        data class NoData(val period: String) : Failure()
        data class EmailFailure(val failed: List<String>) : Failure()
        data class SystemError(val error: Throwable) : Failure()
    }
}

// リファクタリングされたサービス
class ReportGenerator(
    private val dataSource: DataSource,
    private val formatter: ReportFormatter,
    private val emailSender: EmailSender,
    private val logger: Logger
) {
    suspend fun generateAndSendReport(request: ReportRequest): ReportResult {
        return runCatching {
            // データ取得
            val records = fetchRecords(request)
                ?: return ReportResult.Failure.NoData(
                    "${request.startDate} to ${request.endDate}"
                )

            // レポート生成
            val report = createReport(request, records)

            // フォーマット
            val formatted = formatter.format(report, request.format)

            // メール送信（非同期）
            val emailResults = sendEmails(request.recipients, formatted, request)

            // 結果
            val failedEmails = emailResults.filterIsInstance<EmailResult.Failure>()
            if (failedEmails.isNotEmpty()) {
                ReportResult.Failure.EmailFailure(
                    failedEmails.map { it.recipient }
                )
            } else {
                ReportResult.Success(report, emailResults.size)
            }

        }.getOrElse { error ->
            logger.error("Report generation failed", error)
            ReportResult.Failure.SystemError(error)
        }
    }

    private suspend fun fetchRecords(request: ReportRequest): List<DataRecord>? {
        return dataSource.fetchRecords(request.startDate, request.endDate)
            .takeIf { it.isNotEmpty() }
    }

    private fun createReport(
        request: ReportRequest,
        records: List<DataRecord>
    ): Report {
        val summary = records
            .groupBy { it.category }
            .mapValues { (_, records) -> records.sumOf { it.amount } }

        val total = records.sumOf { it.amount }

        return Report(
            startDate = request.startDate,
            endDate = request.endDate,
            recordCount = records.size,
            summary = summary,
            total = total
        )
    }

    private suspend fun sendEmails(
        recipients: List<String>,
        content: String,
        request: ReportRequest
    ): List<EmailResult> = coroutineScope {
        recipients.map { recipient ->
            async {
                sendEmail(recipient, content, request)
            }
        }.awaitAll()
    }

    private suspend fun sendEmail(
        recipient: String,
        content: String,
        request: ReportRequest
    ): EmailResult {
        return runCatching {
            emailSender.send(
                to = recipient,
                subject = "Report for ${request.startDate} to ${request.endDate}",
                body = content
            )
            EmailResult.Success(recipient)
        }.getOrElse { error ->
            logger.warn("Failed to send email to $recipient", error)
            EmailResult.Failure(recipient, error.message ?: "Unknown error")
        }
    }
}

sealed class EmailResult {
    data class Success(val recipient: String) : EmailResult()
    data class Failure(val recipient: String, val reason: String) : EmailResult()
}

// 使用例
suspend fun main() {
    val generator = ReportGenerator(dataSource, formatter, emailSender, logger)

    val request = ReportRequest(
        startDate = LocalDate.now().minusDays(30),
        endDate = LocalDate.now(),
        format = ReportFormat.PDF,
        recipients = listOf("user1@example.com", "user2@example.com")
    )

    when (val result = generator.generateAndSendReport(request)) {
        is ReportResult.Success -> {
            println("Report generated successfully")
            println("Total: ${result.report.total}")
            println("Emails sent: ${result.emailsSent}")
        }
        is ReportResult.Failure.NoData -> {
            println("No data available for period: ${result.period}")
        }
        is ReportResult.Failure.EmailFailure -> {
            println("Failed to send emails to: ${result.failed.joinToString()}")
        }
        is ReportResult.Failure.SystemError -> {
            println("System error: ${result.error.message}")
        }
    }
}
```

## まとめ

本章では、実践的なJavaコードのKotlinへのリファクタリングを学びました：

✅ **デザインパターンのKotlin化（Builder, Factory, Strategy）**
✅ **ボイラープレートコードの大幅な削減**
✅ **可読性と保守性の向上**
✅ **型安全なエラーハンドリング**
✅ **関数型プログラミングスタイルの活用**
✅ **コルーチンによる非同期処理の改善**

これらの技術を組み合わせることで、Javaコードを大幅に簡潔で安全なKotlinコードに変換できます。

## 最終課題

あなたの実際のプロジェクトから、以下のいずれかを選んでKotlinにリファクタリングしてください：

1. 複雑なビジネスロジックを持つサービスクラス
2. 多くのボイラープレートを含むエンティティクラス群
3. エラーハンドリングが複雑なAPIクライアント

リファクタリング時のチェックリスト：
- [ ] データクラスの活用
- [ ] Null安全性の確保
- [ ] 関数型スタイルのコレクション操作
- [ ] 適切なエラーハンドリング（sealed classまたはResult型）
- [ ] コルーチンによる非同期処理
- [ ] 拡張関数の活用
- [ ] デフォルト引数と名前付き引数の使用
- [ ] スコープ関数の適切な使用