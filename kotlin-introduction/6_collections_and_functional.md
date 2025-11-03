# 第6章：コレクションと関数型操作

## 6.1 コレクションフレームワーク

### List、Set、Mapの使い方

Kotlinのコレクションは、読み取り専用（immutable）と可変（mutable）の2種類に分かれています。

#### List - 順序付きコレクション

```kotlin
// 読み取り専用リスト
val readOnlyList: List<String> = listOf("A", "B", "C")
// readOnlyList.add("D")  // コンパイルエラー！

// 可変リスト
val mutableList: MutableList<String> = mutableListOf("A", "B", "C")
mutableList.add("D")  // OK
mutableList.remove("A")  // OK

// 型推論
val numbers = listOf(1, 2, 3, 4, 5)  // List<Int>

// 空のリスト
val emptyList = emptyList<String>()
val anotherEmptyList: List<Int> = listOf()

// リストの操作
val first = numbers.first()  // 1
val last = numbers.last()  // 5
val element = numbers[2]  // 3（インデックスアクセス）
val subList = numbers.subList(1, 3)  // [2, 3]

// 安全なアクセス
val firstOrNull = numbers.firstOrNull()  // 1 または null
val elementOrNull = numbers.getOrNull(10)  // null
val elementOrDefault = numbers.getOrElse(10) { -1 }  // -1
```

#### Set - 重複を許さないコレクション

```kotlin
// 読み取り専用セット
val readOnlySet: Set<String> = setOf("A", "B", "C", "A")  // [A, B, C]

// 可変セット
val mutableSet: MutableSet<String> = mutableSetOf("A", "B", "C")
mutableSet.add("D")  // true
mutableSet.add("A")  // false（既に存在）

// セットの操作
val contains = readOnlySet.contains("A")  // true
val size = readOnlySet.size  // 3

// HashSet（順序保証なし）
val hashSet = hashSetOf("A", "B", "C")

// LinkedHashSet（挿入順序を保持）
val linkedSet = linkedSetOf("C", "A", "B")  // [C, A, B]

// SortedSet（ソート済み）
val sortedSet = sortedSetOf("C", "A", "B")  // [A, B, C]
```

#### Map - キーと値のペア

```kotlin
// 読み取り専用マップ
val readOnlyMap: Map<String, Int> = mapOf(
    "one" to 1,
    "two" to 2,
    "three" to 3
)

// 可変マップ
val mutableMap: MutableMap<String, Int> = mutableMapOf(
    "one" to 1,
    "two" to 2
)
mutableMap["three"] = 3  // 追加
mutableMap["one"] = 10  // 更新
mutableMap.remove("two")  // 削除

// マップの操作
val value = readOnlyMap["one"]  // 1（nullable）
val valueOrDefault = readOnlyMap.getOrDefault("four", 0)  // 0
val valueOrElse = readOnlyMap.getOrElse("four") { -1 }  // -1

// キーと値の取得
val keys = readOnlyMap.keys  // [one, two, three]
val values = readOnlyMap.values  // [1, 2, 3]
val entries = readOnlyMap.entries  // Set<Map.Entry<String, Int>>

// イテレーション
for ((key, value) in readOnlyMap) {
    println("$key -> $value")
}

// HashMap、LinkedHashMap、SortedMap
val hashMap = hashMapOf("A" to 1, "B" to 2)
val linkedMap = linkedMapOf("C" to 3, "A" to 1, "B" to 2)  // 挿入順序保持
val sortedMap = sortedMapOf("C" to 3, "A" to 1, "B" to 2)  // キーでソート
```

**Javaとの比較：**

```java
// Java - 可変・不変の区別が明確でない
List<String> list = List.of("A", "B", "C");  // Java 9+ 不変
List<String> mutableList = new ArrayList<>(List.of("A", "B", "C"));

Set<String> set = Set.of("A", "B", "C");  // Java 9+ 不変
Set<String> mutableSet = new HashSet<>(Set.of("A", "B", "C"));

Map<String, Integer> map = Map.of("one", 1, "two", 2);  // Java 9+ 不変
Map<String, Integer> mutableMap = new HashMap<>(Map.of("one", 1, "two", 2));
```

### Mutableとimmutableの区別

```kotlin
// 読み取り専用（immutable）インターフェース
interface List<out E> {
    val size: Int
    fun get(index: Int): E
    fun contains(element: E): Boolean
    // 変更メソッドなし
}

// 可変（mutable）インターフェース
interface MutableList<E> : List<E> {
    fun add(element: E): Boolean
    fun remove(element: E): Boolean
    fun clear()
    // 変更メソッドあり
}

// 実践的な使用例
class UserRepository {
    private val users = mutableListOf<User>()

    // 外部には読み取り専用として公開
    fun getAllUsers(): List<User> = users.toList()

    // 内部では可変リストとして使用
    fun addUser(user: User) {
        users.add(user)
    }

    fun removeUser(userId: Long) {
        users.removeIf { it.id == userId }
    }
}

// リストのコピー
val original = mutableListOf(1, 2, 3)
val copy = original.toList()  // 読み取り専用のコピー
val mutableCopy = original.toMutableList()  // 可変のコピー

// ダウンキャストは避けるべき
val readOnlyList: List<String> = mutableListOf("A", "B", "C")
// (readOnlyList as MutableList).add("D")  // 技術的には可能だが推奨されない

// より安全なアプローチ
fun processItems(items: List<String>) {
    // itemsは読み取り専用として扱う
    items.forEach { println(it) }

    // 変更が必要な場合はコピーを作成
    val modifiable = items.toMutableList()
    modifiable.add("New Item")
}
```

### JavaコレクションとのブリッジAPI

```kotlin
// KotlinとJavaのコレクション相互運用

// Kotlinリスト → Javaリスト（自動変換）
fun kotlinToJava() {
    val kotlinList: List<String> = listOf("A", "B", "C")
    val javaList: java.util.List<String> = kotlinList  // 自動変換

    // Javaメソッドを呼び出し
    javaService.processList(kotlinList)
}

// Javaリスト → Kotlinリスト
fun javaToKotlin(javaList: java.util.List<String>) {
    // プラットフォーム型として扱われる
    val kotlinList: List<String> = javaList

    // 安全な変換
    val safeCopy = javaList.toList()  // 読み取り専用コピー
    val mutableCopy = javaList.toMutableList()  // 可変コピー
}

// Java配列 → Kotlinリスト
fun arrayToList(javaArray: Array<String>) {
    val list = javaArray.toList()
    val mutableList = javaArray.toMutableList()
}

// Kotlinリスト → Java配列
fun listToArray() {
    val kotlinList = listOf("A", "B", "C")
    val array: Array<String> = kotlinList.toTypedArray()

    // プリミティブ配列
    val intList = listOf(1, 2, 3)
    val intArray: IntArray = intList.toIntArray()
}

// Javaコレクションのnull安全な処理
fun processJavaCollection(javaList: java.util.List<String>?) {
    // nullチェック
    val safeList = javaList?.toList() ?: emptyList()

    // null要素のフィルタリング
    val nonNullList = javaList?.filterNotNull() ?: emptyList()
}

// Java 8 Stream → Kotlinシーケンス
fun streamToSequence(stream: java.util.stream.Stream<String>) {
    val sequence = stream.iterator().asSequence()
    sequence.forEach { println(it) }
}
```

## 6.2 コレクション操作

### filter、map、flatMap

```kotlin
// filter - 条件に合う要素を抽出
val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
val evenNumbers = numbers.filter { it % 2 == 0 }  // [2, 4, 6, 8, 10]
val oddNumbers = numbers.filterNot { it % 2 == 0 }  // [1, 3, 5, 7, 9]

// filterIsInstance - 特定の型の要素のみ抽出
val mixed: List<Any> = listOf(1, "two", 3, "four", 5)
val strings = mixed.filterIsInstance<String>()  // [two, four]
val integers = mixed.filterIsInstance<Int>()  // [1, 3, 5]

// filterNotNull - null要素を除去
val nullableList: List<String?> = listOf("A", null, "B", null, "C")
val nonNullList: List<String> = nullableList.filterNotNull()  // [A, B, C]

// map - 各要素を変換
val doubled = numbers.map { it * 2 }  // [2, 4, 6, 8, 10, ...]
val strings = numbers.map { "Number: $it" }

// mapNotNull - 変換してnullを除外
val parsed = listOf("1", "two", "3", "four")
    .mapNotNull { it.toIntOrNull() }  // [1, 3]

// mapIndexed - インデックス付きで変換
val indexed = numbers.mapIndexed { index, value ->
    "[$index] = $value"
}

// flatMap - ネストしたコレクションを平坦化
val nestedLists = listOf(
    listOf(1, 2, 3),
    listOf(4, 5),
    listOf(6, 7, 8, 9)
)
val flattened = nestedLists.flatMap { it }  // [1, 2, 3, 4, 5, 6, 7, 8, 9]

// 実践的な例
data class User(val name: String, val emails: List<String>)

val users = listOf(
    User("Alice", listOf("alice@example.com", "alice@work.com")),
    User("Bob", listOf("bob@example.com")),
    User("Charlie", listOf("charlie@example.com", "charlie@personal.com"))
)

val allEmails = users.flatMap { it.emails }
// [alice@example.com, alice@work.com, bob@example.com, ...]

// flatten - ネストしたコレクションを平坦化（flatMapの特殊ケース）
val lists = listOf(listOf(1, 2), listOf(3, 4), listOf(5))
val flat = lists.flatten()  // [1, 2, 3, 4, 5]
```

**Javaとの比較：**

```java
// Java Stream API
List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// filter
List<Integer> evenNumbers = numbers.stream()
    .filter(n -> n % 2 == 0)
    .collect(Collectors.toList());

// map
List<Integer> doubled = numbers.stream()
    .map(n -> n * 2)
    .collect(Collectors.toList());

// flatMap
List<List<Integer>> nested = List.of(
    List.of(1, 2, 3),
    List.of(4, 5),
    List.of(6, 7, 8, 9)
);
List<Integer> flattened = nested.stream()
    .flatMap(List::stream)
    .collect(Collectors.toList());
```

### groupBy、partition

```kotlin
// groupBy - 条件でグループ化
val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
val grouped = numbers.groupBy { it % 3 }
// {1=[1, 4, 7, 10], 2=[2, 5, 8], 0=[3, 6, 9]}

// 実践的な例
data class Person(val name: String, val age: Int, val city: String)

val people = listOf(
    Person("Alice", 25, "Tokyo"),
    Person("Bob", 30, "Osaka"),
    Person("Charlie", 25, "Tokyo"),
    Person("David", 30, "Tokyo"),
    Person("Eve", 25, "Osaka")
)

// 都市でグループ化
val byCity = people.groupBy { it.city }
// {Tokyo=[Alice, Charlie, David], Osaka=[Bob, Eve]}

// 年齢でグループ化
val byAge = people.groupBy { it.age }
// {25=[Alice, Charlie, Eve], 30=[Bob, David]}

// 年代でグループ化
val byAgeGroup = people.groupBy { it.age / 10 }
// {2=[Alice, Bob, Charlie, David, Eve]}

// groupBy with transformation
val namesByCity = people.groupBy(
    keySelector = { it.city },
    valueTransform = { it.name }
)
// {Tokyo=[Alice, Charlie, David], Osaka=[Bob, Eve]}

// partition - 条件で2つに分割
val (adults, minors) = people.partition { it.age >= 20 }
// adults: 20歳以上
// minors: 20歳未満

val (even, odd) = numbers.partition { it % 2 == 0 }
// even: [2, 4, 6, 8, 10]
// odd: [1, 3, 5, 7, 9]

// より複雑な例：集計とグループ化
val orderStats = people.groupBy { it.city }
    .mapValues { (city, persons) ->
        mapOf(
            "count" to persons.size,
            "avgAge" to persons.map { it.age }.average(),
            "names" to persons.map { it.name }
        )
    }
```

### reduce、fold、aggregate

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)

// reduce - 左から順に要素を結合
val sum = numbers.reduce { acc, num -> acc + num }  // 15
val product = numbers.reduce { acc, num -> acc * num }  // 120

// fold - 初期値を指定して結合
val sumWithInitial = numbers.fold(0) { acc, num -> acc + num }  // 15
val sumStartingFrom10 = numbers.fold(10) { acc, num -> acc + num }  // 25

// 実践的な例：文字列の結合
val words = listOf("Kotlin", "is", "awesome")
val sentence = words.reduce { acc, word -> "$acc $word" }
// "Kotlin is awesome"

val sentenceWithPrefix = words.fold("I think:") { acc, word ->
    "$acc $word"
}
// "I think: Kotlin is awesome"

// 複雑なオブジェクトの集計
data class Order(val id: Long, val amount: Double, val tax: Double)

val orders = listOf(
    Order(1, 100.0, 10.0),
    Order(2, 200.0, 20.0),
    Order(3, 150.0, 15.0)
)

// 合計金額の計算
val totalAmount = orders.fold(0.0) { total, order ->
    total + order.amount + order.tax
}  // 495.0

// より詳細な集計
data class OrderSummary(
    val totalAmount: Double = 0.0,
    val totalTax: Double = 0.0,
    val orderCount: Int = 0
)

val summary = orders.fold(OrderSummary()) { acc, order ->
    OrderSummary(
        totalAmount = acc.totalAmount + order.amount,
        totalTax = acc.totalTax + order.tax,
        orderCount = acc.orderCount + 1
    )
}

// reduceRight / foldRight - 右から結合
val rightReduced = numbers.reduceRight { num, acc -> num - acc }
// 1 - (2 - (3 - (4 - 5))) = 3

// reduceIndexed / foldIndexed - インデックス付き
val indexed = numbers.foldIndexed("") { index, acc, num ->
    "$acc[$index]=$num "
}
// "[0]=1 [1]=2 [2]=3 [3]=4 [4]=5 "

// runningFold - 各ステップの結果を保持
val cumulative = numbers.runningFold(0) { acc, num -> acc + num }
// [0, 1, 3, 6, 10, 15] (累積和)

// runningReduce
val runningSum = numbers.runningReduce { acc, num -> acc + num }
// [1, 3, 6, 10, 15]
```

**Javaとの比較：**

```java
// Java Stream API
List<Integer> numbers = List.of(1, 2, 3, 4, 5);

// reduce
int sum = numbers.stream()
    .reduce(0, (acc, num) -> acc + num);

// または
int sum2 = numbers.stream()
    .reduce(0, Integer::sum);

// collect（より複雑な集計）
OrderSummary summary = orders.stream()
    .collect(
        OrderSummary::new,
        (acc, order) -> {
            acc.totalAmount += order.getAmount();
            acc.totalTax += order.getTax();
            acc.orderCount++;
        },
        (acc1, acc2) -> {
            // 並列処理の結合
        }
    );
```

### その他の便利な操作

```kotlin
val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

// take / drop - 先頭から取得/スキップ
val firstThree = numbers.take(3)  // [1, 2, 3]
val exceptFirstThree = numbers.drop(3)  // [4, 5, 6, 7, 8, 9, 10]

// takeLast / dropLast - 末尾から
val lastThree = numbers.takeLast(3)  // [8, 9, 10]
val exceptLastThree = numbers.dropLast(3)  // [1, 2, 3, 4, 5, 6, 7]

// takeWhile / dropWhile - 条件が成立する間
val takeWhileLessThan5 = numbers.takeWhile { it < 5 }  // [1, 2, 3, 4]
val dropWhileLessThan5 = numbers.dropWhile { it < 5 }  // [5, 6, 7, 8, 9, 10]

// distinct - 重複を除去
val duplicates = listOf(1, 2, 2, 3, 3, 3, 4, 4, 5)
val unique = duplicates.distinct()  // [1, 2, 3, 4, 5]

// distinctBy - キーで重複を除去
data class User(val id: Long, val name: String, val email: String)
val users = listOf(
    User(1, "Alice", "alice@example.com"),
    User(2, "Bob", "bob@example.com"),
    User(3, "Alice", "alice2@example.com")
)
val uniqueByName = users.distinctBy { it.name }  // id=1,2のみ

// sorted - ソート
val sorted = listOf(3, 1, 4, 1, 5, 9).sorted()  // [1, 1, 3, 4, 5, 9]
val sortedDesc = numbers.sortedDescending()  // [10, 9, 8, 7, ...]

// sortedBy - キーでソート
val sortedByAge = users.sortedBy { it.name }
val sortedByAgeDesc = users.sortedByDescending { it.id }

// sortedWith - カスタムComparator
val customSorted = users.sortedWith(
    compareBy<User> { it.name }.thenBy { it.id }
)

// reversed - 逆順
val reversed = numbers.reversed()  // [10, 9, 8, 7, ...]

// shuffled - ランダムシャッフル
val shuffled = numbers.shuffled()

// zip - 2つのリストを結合
val names = listOf("Alice", "Bob", "Charlie")
val ages = listOf(25, 30, 35)
val pairs = names.zip(ages)  // [(Alice, 25), (Bob, 30), (Charlie, 35)]

val combined = names.zip(ages) { name, age ->
    "$name is $age years old"
}
// ["Alice is 25 years old", ...]

// unzip - Pairのリストを2つのリストに分割
val pairList = listOf("A" to 1, "B" to 2, "C" to 3)
val (letters, nums) = pairList.unzip()
// letters: [A, B, C], nums: [1, 2, 3]

// windowed - スライディングウィンドウ
val windowed = numbers.windowed(size = 3, step = 1)
// [[1,2,3], [2,3,4], [3,4,5], ...]

val windowed2 = numbers.windowed(size = 3, step = 2)
// [[1,2,3], [3,4,5], [5,6,7], [7,8,9], [9,10]]

// chunked - 指定サイズのチャンクに分割
val chunked = numbers.chunked(3)
// [[1, 2, 3], [4, 5, 6], [7, 8, 9], [10]]

// associate - リストからマップを作成
val nameMap = users.associate { it.id to it.name }
// {1=Alice, 2=Bob, 3=Alice}

val emailMap = users.associateBy { it.id }
// {1=User(...), 2=User(...), 3=User(...)}

val idMap = users.associateWith { it.id }
// {User(...)=1, User(...)=2, User(...)=3}
```

## 6.3 シーケンスによる遅延評価

### Java Stream APIとの比較

```kotlin
// コレクション（即時評価）
val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

// 各操作が即座に実行され、中間リストが作成される
val result1 = numbers
    .filter { println("filter: $it"); it % 2 == 0 }
    .map { println("map: $it"); it * 2 }
    .take(2)
// 出力: すべての要素に対してfilterが実行され、その後mapが実行される

// シーケンス（遅延評価）
val result2 = numbers.asSequence()
    .filter { println("filter: $it"); it % 2 == 0 }
    .map { println("map: $it"); it * 2 }
    .take(2)
    .toList()
// 出力: 必要な要素だけが処理される（より効率的）

// パフォーマンス比較
val largeList = (1..1_000_000).toList()

// コレクション（中間リストを作成）
val collectionResult = largeList
    .filter { it % 2 == 0 }
    .map { it * 2 }
    .take(10)

// シーケンス（中間リストなし）
val sequenceResult = largeList.asSequence()
    .filter { it % 2 == 0 }
    .map { it * 2 }
    .take(10)
    .toList()
```

**Javaとの比較：**

```java
// Java Stream（遅延評価）
List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

List<Integer> result = numbers.stream()
    .filter(n -> { System.out.println("filter: " + n); return n % 2 == 0; })
    .map(n -> { System.out.println("map: " + n); return n * 2; })
    .limit(2)
    .collect(Collectors.toList());
```

### パフォーマンスの最適化

```kotlin
// シーケンスを使うべき場合
// 1. 大量のデータを扱う場合
val largeData = (1..1_000_000).asSequence()
    .filter { it % 2 == 0 }
    .map { it * 2 }
    .take(100)
    .toList()

// 2. 複数の変換操作を連鎖させる場合
val result = data.asSequence()
    .filter { it.isValid }
    .map { it.process() }
    .filter { it.isNotEmpty() }
    .map { it.transform() }
    .toList()

// コレクションを使うべき場合
// 1. 小さなコレクション（100要素未満）
val small = listOf(1, 2, 3, 4, 5)
    .filter { it % 2 == 0 }
    .map { it * 2 }

// 2. インデックスアクセスが必要な場合
val indexed = list.filter { it > 0 }[3]  // OK
// val indexed = list.asSequence().filter { it > 0 }[3]  // NG

// シーケンスの生成
// generateSequence - 無限シーケンス
val fibonacci = generateSequence(1 to 1) { (a, b) ->
    b to (a + b)
}.map { it.first }

val first10Fib = fibonacci.take(10).toList()
// [1, 1, 2, 3, 5, 8, 13, 21, 34, 55]

// sequence builder
val oddNumbers = sequence {
    var i = 1
    while (true) {
        yield(i)
        i += 2
    }
}

val first5Odd = oddNumbers.take(5).toList()  // [1, 3, 5, 7, 9]

// yieldAll - 複数の値を生成
val combined = sequence {
    yieldAll(listOf(1, 2, 3))
    yield(4)
    yieldAll(generateSequence(5) { it + 1 }.take(3))
}
combined.toList()  // [1, 2, 3, 4, 5, 6, 7]
```

### 無限シーケンスの扱い

```kotlin
// 無限シーケンスの生成
val infiniteNumbers = generateSequence(0) { it + 1 }

// take で有限化
val first100 = infiniteNumbers.take(100).toList()

// takeWhile で条件付き有限化
val lessThan1000 = infiniteNumbers.takeWhile { it < 1000 }.toList()

// 実践的な例：ページング処理
fun fetchPages(): Sequence<List<User>> = sequence {
    var page = 0
    while (true) {
        val users = fetchUsersFromApi(page)
        if (users.isEmpty()) break
        yield(users)
        page++
    }
}

val allUsers = fetchPages()
    .flatMap { it.asSequence() }
    .take(1000)  // 最大1000ユーザー
    .toList()

// 素数の無限シーケンス
fun primes() = sequence {
    var numbers = generateSequence(2) { it + 1 }

    while (true) {
        val prime = numbers.first()
        yield(prime)
        numbers = numbers.filter { it % prime != 0 }
    }
}

val first10Primes = primes().take(10).toList()
// [2, 3, 5, 7, 11, 13, 17, 19, 23, 29]

// ファイルの行単位読み込み（メモリ効率的）
fun processLargeFile(path: String) {
    File(path).useLines { lines ->
        lines
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .filter { it.startsWith("ERROR") }
            .forEach { println(it) }
    }
}
```

## まとめ

本章では、Kotlinのコレクションと関数型操作について学びました：

✅ **immutableとmutableの明確な区別**
✅ **豊富なコレクション操作関数（filter, map, flatMap, groupBy等）**
✅ **reduce/foldによる強力な集約処理**
✅ **シーケンスによる遅延評価とパフォーマンス最適化**
✅ **JavaコレクションAPIとのシームレスな相互運用**

これらの機能により、Kotlinでは関数型プログラミングスタイルで、簡潔かつ効率的なコレクション処理を実現できます。

## 演習問題

1. 以下の要件を満たす関数を実装してください：
   - 文字列のリストを受け取る
   - 長さが3以上の文字列のみを抽出
   - すべて大文字に変換
   - アルファベット順にソート
   - 結果を返す

2. トランザクションのリスト（金額、カテゴリ、日付を持つ）から、カテゴリごとの合計金額を計算し、合計金額の降順でマップを返す関数を実装してください。

3. 無限シーケンスを使って、フィボナッチ数列の中で1000以下の偶数のみを抽出する処理を実装してください。