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

#### 比較図解
```
┌─────────────────────────────────────────┐
│ Rectangle(15.0) の場合                   │
└─────────────────────────────────────────┘
  引数1つ
    ↓
  セカンダリコンストラクタ: constructor(size: Double)
    ↓
  : this(size, size)  ← プライマリへ委譲
    ↓
  プライマリコンストラクタ: Rectangle(15.0, 15.0)
    ↓
  width = 15.0, height = 15.0


┌─────────────────────────────────────────┐
│ Rectangle(10.0, 20.0) の場合             │
└─────────────────────────────────────────┘
  引数2つ
    ↓
  プライマリコンストラクタ: Rectangle(10.0, 20.0)
    ↓
  width = 10.0, height = 20.0
  （セカンダリコンストラクタは通らない）
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

#### Kotlinではdefaultキーワードが不要で、実装をそのまま書けます。

```kotlin
interface Clickable {
    fun click()  // 抽象メソッド

    fun showOff() = println("I'm clickable!")  // デフォルト実装
}
```

```java
// Java 8以降
interface Clickable {
    void click();  // 抽象メソッド
    
    default void showOff() {  // デフォルト実装
        System.out.println("I'm clickable!");
    }
}
```

実用例
```kotlin
class SimpleButton : Clickable {
    override fun click() = println("Simple clicked")
    // showOff()はオーバーライドしなくてもOK
}

val button = SimpleButton()
button.click()    // "Simple clicked"
button.showOff()  // "I'm clickable!"（インターフェースの実装を使用）
```

#### 複数インターフェースの実装
複数のインターフェースに同じ名前のメソッドがあると、どちらを使うか曖昧になります（ダイヤモンド問題）。
Kotlinはこれを防ぐため、明示的なオーバーライドを要求します。
```kotlin
interface Clickable {
    fun showOff() = println("I'm clickable!")
}

interface Focusable {
    fun showOff() = println("I'm focusable!")
}

class Button : Clickable, Focusable {
    // 同名のメソッドがある場合は明示的に実装が必要
    override fun showOff() {
        super<Clickable>.showOff()   // ClickableのshowOff()を呼ぶ
        super<Focusable>.showOff()   // FocusableのshowOff()を呼ぶ
        println("I'm a button!")
    }
}
```

実行結果
```
val button = Button()
button.showOff()
// 出力:
// I'm clickable!
// I'm focusable!
// I'm a button!
```

#### プロパティを持つインターフェース

- 抽象プロパティ（name）
    - 実装クラスで値を提供する必要がある
    - バッキングフィールドを持たない

- 計算プロパティ（displayName）
    - インターフェースがgetterの実装を提供
    - nameを使って動的に値を計算
    - バッキングフィールドを持たない
```kotlin
interface Named {
    val name: String  // 抽象プロパティ

    val displayName: String
        get() = name.uppercase()  // 計算プロパティ
}

class Person(override val name: String) : Named
```

実装例
```kotlin
class Person(override val name: String) : Named

val person = Person("alice")
println(person.name)         // "alice"
println(person.displayName)  // "ALICE"（自動的に大文字変換）
```

### プロパティの委譲

Kotlinはプロパティの振る舞いを委譲できる強力な機能を提供します。
プロパティの委譲とはプロパティのgetter/setterの振る舞いを別のオブジェクトに任せる機能です。byキーワードを使って実現します。

基本構文
```kotlin
class Example {
    var property: Type by DelegateObject()
    //                   ↑
    //              この部分が「委譲先」
}
```

1. lazy（遅延初期化委譲）
```kotlin
class HeavyObject {
    val config: Configuration by lazy {
        println("Loading configuration...")
        loadConfiguration()
    }
}
```

解説
- 初回アクセス時に1度だけ実行される
- 結果はキャッシュされる（2回目以降は計算しない）
- val（読み取り専用）でのみ使用可能
- スレッドセーフ（デフォルト）

動作例
```kotlin
val obj = HeavyObject()
// まだ何も実行されていない

println(obj.config)  // "Loading configuration..." が出力され、Configurationを返す
println(obj.config)  // キャッシュされた値を返す（"Loading..."は出力されない）
```

2. observable（変更監視委譲）
```kotlin
import kotlin.properties.Delegates

class User {
    var name: String by Delegates.observable("<no name>") {
        prop, old, new ->
        println("$old -> $new")
    }
}
```

解説
- プロパティの値が変更されるたびにコールバックが実行される
- 初期値（"<no name>"）を設定できる
- ラムダの引数
    - prop: プロパティの情報（KProperty<*>）
    - old: 変更前の値
    - new: 変更後の値

動作例
```kotlin
val user = User()
println(user.name)  // "<no name>"

user.name = "Alice"  // 出力: <no name> -> Alice
user.name = "Bob"    // 出力: Alice -> Bob
user.name = "Bob"    // 出力: Bob -> Bob（同じ値でも実行される）
```

3. vetoable（条件付き変更委譲）
```kotlin
class Product {
    var price: Double by Delegates.vetoable(0.0) {
        prop, old, new ->
        new >= 0  // 負の値は拒否（false を返すと変更されない）
    }
}
```

解説
- ラムダが**trueを返した場合のみ値が変更される**
- falseを返すと変更がキャンセルされる
- バリデーション（検証）に便利

動作例
```kotlin
val product = Product()
println(product.price)  // 0.0

product.price = 100.0
println(product.price)  // 100.0（正の値なので変更成功）

product.price = -50.0   // 負の値なので拒否
println(product.price)  // 100.0（変更されていない）
```

4. Map による委譲
```kotlin
class UserData(map: Map<String, Any?>) {
    val name: String by map
    val age: Int by map
    val email: String by map
}
```

解説
- Mapのキーと値をプロパティとして扱う
- プロパティ名が自動的にMapのキーになる
- JSON/設定ファイルのパース結果を扱うのに便利

動作例
```kotlin
val userData = UserData(mapOf(
    "name" to "Alice",
    "age" to 25,
    "email" to "alice@example.com"
))

println(userData.name)   // "Alice"（map["name"] を取得）
println(userData.age)    // 25（map["age"] を取得）
println(userData.email)  // "alice@example.com"
```


## 4.3 データクラスとシールドクラス

### データクラス

#### データクラスの基本制約
```kotlin
data class Point(val x: Int, val y: Int)
```
#### データクラスの必須要件
1. プライマリコンストラクタに少なくとも1つのパラメータが必要
```kotlin
// ✅ OK
data class Point(val x: Int, val y: Int)

// ❌ エラー: パラメータがない
data class Empty()

// ✅ OK: 最低1つあればOK
data class Single(val value: String)
```
2. プライマリコンストラクタのパラメータはvalまたはvarが必要
```kotlin
// ❌ エラー: valもvarもない
data class Wrong(x: Int, y: Int)

// ✅ OK
data class Right(val x: Int, val y: Int)
```
3. 自動生成されるメソッド
- equals() / hashCode(): 値の比較
- toString(): 文字列表現
- copy(): コピーを作成
- componentN(): 分解宣言用

4. その他の制約
```kotlin
// ❌ データクラスはabstractにできない
abstract data class Base(val x: Int)

// ❌ データクラスはopenにできない（継承不可）
open data class Base(val x: Int)

// ❌ データクラスはsealedにできない
sealed data class Result(val value: Int)

// ✅ OK: 他のクラスを継承できる（インターフェース実装も可能）
interface Named {
    val name: String
}
data class Person(override val name: String, val age: Int) : Named
```
#### デフォルト値の設定
解説
- プロパティにデフォルト値を設定できる
- インスタンス作成時に省略可能
- 名前付き引数と組み合わせると非常に便利

```kotlin
data class Configuration(
    val host: String = "localhost",
    val port: Int = 8080,
    val secure: Boolean = false
)

```
使用例
```kotlin
// すべてデフォルト値
val config1 = Configuration()
println(config1)  // Configuration(host=localhost, port=8080, secure=false)

// 一部だけ指定（名前付き引数を使用）
val config2 = Configuration(host = "example.com")
println(config2)  // Configuration(host=example.com, port=8080, secure=false)

// 順番を気にせず指定可能
val config3 = Configuration(port = 443, secure = true)
println(config3)  // Configuration(host=localhost, port=443, secure=true)

// すべて指定
val config4 = Configuration(
    host = "api.example.com",
    port = 443,
    secure = true
)

// copy()との組み合わせ
val original = Configuration()
// 一部だけ変更したコピーを作成
val modified = original.copy(secure = true)

println(original)  // Configuration(host=localhost, port=8080, secure=false)
println(modified)  // Configuration(host=localhost, port=8080, secure=true)
```

#### 分解宣言（Destructuring Declaration）
```kotlin
data class Person(val name: String, val age: Int, val city: String)

val person = Person("Alice", 25, "Tokyo")
val (name, age) = person  // cityは無視される
```
解説
- データクラスのプロパティを複数の変数に一度に代入できる
- 内部的にはcomponent1(), component2(), ... が呼ばれる
- 最初の5つのプロパティまでサポート（component1〜component5）

動作の仕組み
```kotlin
// 分解宣言の裏側
val (name, age) = person

// ↓ 実際にはこのように展開される
val name = person.component1()  // person.name
val age = person.component2()   // person.age
```

使用例
```kotlin
data class User(val id: Int, val name: String, val email: String, val age: Int)

val user = User(1, "Alice", "alice@example.com", 25)

// 必要な数だけ取り出せる
val (id) = user                        // idだけ
val (id, name) = user                  // idとname
val (id, name, email) = user           // id、name、email
val (id, name, email, age) = user      // すべて

// 不要な要素はアンダースコアで無視
val (_, name, email) = user            // idを無視
val (id, _, _, age) = user             // nameとemailを無視
```

5つ以上のプロパティがある場合
```kotlin
data class LargeData(
    val a: Int, val b: Int, val c: Int,
    val d: Int, val e: Int, val f: Int
)

val data = LargeData(1, 2, 3, 4, 5, 6)

// 最初の5つまで分解可能
val (v1, v2, v3, v4, v5) = data  // OK

// ❌ 6つ目以降は取得できない（component6は生成されない）
// val (v1, v2, v3, v4, v5, v6) = data  // エラー
```

#### コレクション内のデータクラス
```kotlin
val users = listOf(
    User(1, "Alice", "alice@example.com", 25),
    User(2, "Bob", "bob@example.com", 30),
    User(3, "Charlie", "charlie@example.com", 35)
)
```
ソート（sortedBy）
```kotlin
// 年齢でソート
val sortedByAge = users.sortedBy { it.age }
println(sortedByAge)
// [User(id=1, name=Alice, ..., age=25),
//  User(id=2, name=Bob, ..., age=30),
//  User(id=3, name=Charlie, ..., age=35)]

// 名前でソート
val sortedByName = users.sortedBy { it.name }

// 降順でソート
val sortedByAgeDesc = users.sortedByDescending { it.age }

// 複数条件でソート
val sorted = users.sortedWith(
    compareBy({ it.age }, { it.name })
)
```

グループ化（groupBy）
```kotlin
// 年代（20代、30代など）でグループ化
val grouped = users.groupBy { it.age / 10 }
println(grouped)
// {2=[User(id=1, name=Alice, ..., age=25)],
//  3=[User(id=2, name=Bob, ..., age=30),
//     User(id=3, name=Charlie, ..., age=35)]}

// 20代のユーザーを取得
val twenties = grouped[2]  // List<User>?

// 名前の最初の文字でグループ化
val groupedByInitial = users.groupBy { it.name.first() }
// {'A'=[User(id=1, name=Alice, ...)],
//  'B'=[User(id=2, name=Bob, ...)],
//  'C'=[User(id=3, name=Charlie, ...)]}
```

その他の便利な操作
```kotlin
data class User(val id: Int, val name: String, val email: String, val age: Int)

val users = listOf(
    User(1, "Alice", "alice@example.com", 25),
    User(2, "Bob", "bob@example.com", 30),
    User(3, "Charlie", "charlie@example.com", 35)
)

// フィルタリング
val adults = users.filter { it.age >= 30 }
// [User(id=2, name=Bob, ...), User(id=3, name=Charlie, ...)]

// マッピング
val names = users.map { it.name }
// ["Alice", "Bob", "Charlie"]

// 条件に合う最初の要素を検索
val bob = users.find { it.name == "Bob" }

// 最大値を持つ要素
val oldest = users.maxByOrNull { it.age }
// User(id=3, name=Charlie, ..., age=35)

// 集計
val totalAge = users.sumOf { it.age }  // 90
val averageAge = users.map { it.age }.average()  // 30.0

// パーティション（条件で2つに分ける）
val (young, old) = users.partition { it.age < 30 }
// young: [User(id=1, name=Alice, ...)]
// old: [User(id=2, name=Bob, ...), User(id=3, name=Charlie, ...)]

// 重複排除（equalsベース）
val withDuplicates = listOf(
    User(1, "Alice", "alice@example.com", 25),
    User(1, "Alice", "alice@example.com", 25),
    User(2, "Bob", "bob@example.com", 30)
)
val unique = withDuplicates.distinct()  // 2要素になる

// 特定プロパティでの重複排除
val uniqueByName = users.distinctBy { it.name }
```

### シールドクラス（代数的データ型）

シールドクラスとは？
シールドクラス（Sealed Class）は、限定された継承階層を表現するための特殊なクラスです。すべてのサブクラスがコンパイル時に既知であることが保証されます。
**主な特徴**
- 制限された継承: サブクラスは同じファイルまたは同じパッケージ内でのみ定義可能
- 網羅的チェック: when式ですべてのケースをカバーしているか検証できる
- 型安全: 予期しないサブクラスが存在しないことが保証される

#### 基本的なシールドクラス
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```
解説

sealed class Result<out T>:
- sealedキーワードで制限された継承階層を定義
- <out T>: 共変（covariant）型パラメータ（後で詳しく説明）

サブクラスの種類:
1. data class Success<T>: データを持つ成功状態
2. data class Error: エラー情報を持つ失敗状態
3. object Loading: シングルトンのローディング状態

Result<Nothing>の意味:
- NothingはKotlinのボトム型（すべての型のサブタイプ）
- 「値を持たない」ことを表現
- ErrorとLoadingはデータを返さないためNothingを使用

使用例
```kotlin
fun fetchUser(id: Int): Result<User> {
    return try {
        val user = apiCall(id)
        Result.Success(user)
    } catch (e: Exception) {
        Result.Error(e)
    }
}

// 結果を処理
val result = fetchUser(123)
when (result) {
    is Result.Success -> println("Got user: ${result.data.name}")
    is Result.Error -> println("Failed: ${result.exception.message}")
    Result.Loading -> println("Still loading...")
}
```
#### when式での網羅的チェック
```kotlin
fun <T> handleResult(result: Result<T>) {
    when (result) {
        is Result.Success -> println("Success: ${result.data}")
        is Result.Error -> println("Error: ${result.exception.message}")
        Result.Loading -> println("Loading...")
        // elseは不要（すべてのケースが網羅されている）
    }
}
```
解説
網羅的チェック
- シールドクラスのすべてのサブクラスを処理している場合、elseブランチが不要
- コンパイラがすべてのケースをカバーしているか検証
- 新しいサブクラスを追加すると、コンパイルエラーが発生（安全！）

通常のクラスとの比較
```kotlin
// 通常のクラス（open）
open class Animal {
    class Dog : Animal()
    class Cat : Animal()
}

fun handle(animal: Animal) {
    when (animal) {
        is Animal.Dog -> println("Dog")
        is Animal.Cat -> println("Cat")
        // elseが必要！（他にもサブクラスが存在する可能性がある）
        else -> println("Unknown animal")
    }
}

// シールドクラス
sealed class Animal {
    class Dog : Animal()
    class Cat : Animal()
}

fun handle(animal: Animal) {
    when (animal) {
        is Animal.Dog -> println("Dog")
        is Animal.Cat -> println("Cat")
        // elseは不要（すべてのサブクラスが既知）
    }
}
```

#### 実用例：APIレスポンス
```kotlin
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T, val code: Int = 200) : ApiResponse<T>()
    data class Error(val message: String, val code: Int) : ApiResponse<Nothing>()
    object NetworkError : ApiResponse<Nothing>()
    object Unauthorized : ApiResponse<Nothing>()
}
```

解説
より詳細なステートマシンを表現
- Success: 成功時のデータとステータスコード
- Error: サーバーエラー（メッセージとコード）
- NetworkError: ネットワーク接続エラー
- Unauthorized: 認証エラー（401）

使用例
```kotlin
suspend fun fetchUsers(): ApiResponse<List<User>> {
    return try {
        val response = api.getUsers()
        when (response.code) {
            200 -> ApiResponse.Success(response.body)
            401 -> ApiResponse.Unauthorized
            else -> ApiResponse.Error(response.message, response.code)
        }
    } catch (e: IOException) {
        ApiResponse.NetworkError
    }
}

// UIレイヤーでの処理
fun handleApiResponse(response: ApiResponse<User>) {
    when (response) {
        is ApiResponse.Success -> {
            showUser(response.data)
            showToast("Success (${response.code})")
        }
        is ApiResponse.Error -> {
            showError("Error ${response.code}: ${response.message}")
        }
        ApiResponse.NetworkError -> {
            showError("Check your internet connection")
            showRetryButton()
        }
        ApiResponse.Unauthorized -> {
            redirectToLogin()
        }
    }
}
```
#### シールドインターフェース（Kotlin 1.5+）
```kotlin
sealed interface Operation {
    data class Add(val a: Int, val b: Int) : Operation
    data class Subtract(val a: Int, val b: Int) : Operation
    data class Multiply(val a: Int, val b: Int) : Operation
    data class Divide(val a: Int, val b: Int) : Operation
}
```
<img width="497" height="135" alt="スクリーンショット 2025-11-05 2 15 46" src="https://github.com/user-attachments/assets/fa947a4b-4038-49d4-964a-78817d31b392" />

使用例
```kotlin
fun execute(operation: Operation): Int = when (operation) {
    is Operation.Add -> operation.a + operation.b
    is Operation.Subtract -> operation.a - operation.b
    is Operation.Multiply -> operation.a * operation.b
    is Operation.Divide -> operation.a / operation.b
}

// 実行
val result1 = execute(Operation.Add(10, 5))        // 15
val result2 = execute(Operation.Multiply(3, 4))    // 12
val result3 = execute(Operation.Divide(20, 4))     // 5
```

#### Javaとの比較
Kotlin（シンプル）
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

fun <T> handleResult(result: Result<T>) {
    when (result) {
        is Result.Success -> println("Success: ${result.data}")
        is Result.Error -> println("Error: ${result.exception.message}")
        Result.Loading -> println("Loading...")
    }
}
```
Java 17+（冗長）
```java
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

// パターンマッチング
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
