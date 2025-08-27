# 第7章：インデックスとクエリ最適化

## 7.1 インデックスの仕組みと種類

インデックスは、データベースの検索性能を向上させる重要な仕組みです。本の索引のように、特定のデータを素早く見つけるための道標として機能します。

### インデックスの基本概念

インデックスは、テーブルの特定のカラムの値とその行の物理的な位置を対応付けるデータ構造です。これにより、全件検索（フルテーブルスキャン）を避けて、効率的にデータを検索できます。

```sql
-- インデックスの作成例
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_order_date ON orders(order_date);
CREATE UNIQUE INDEX idx_product_code ON products(product_code);
```

### B-Treeインデックス

最も一般的なインデックス構造で、バランス木構造を採用しています。

![B-Treeインデックスの構造](images/btree-index.svg)

#### B-Treeインデックスの特徴

```sql
-- B-Treeインデックスが効果的なケース
-- 範囲検索
SELECT * FROM orders 
WHERE order_date BETWEEN '2024-01-01' AND '2024-12-31';

-- 前方一致検索
SELECT * FROM customers 
WHERE name LIKE 'Smith%';

-- ソート処理
SELECT * FROM products 
ORDER BY price DESC;
```

#### B-Treeインデックスの性能特性

- 検索速度：O(log n)
- 挿入・削除：O(log n)
- 範囲検索：効率的
- メモリ使用量：中程度

### ハッシュインデックス

ハッシュ関数を使用してキーを直接物理位置にマッピングするインデックスです。

#### ハッシュインデックスの使用例

```sql
-- MySQLのMemoryストレージエンジンでの例
CREATE TABLE session_data (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id INT,
    data TEXT,
    INDEX idx_session USING HASH (session_id)
) ENGINE=MEMORY;

-- 等価検索（ハッシュインデックスが最も効果的）
SELECT * FROM session_data WHERE session_id = 'abc123xyz';
```

#### ハッシュインデックスの制限

```sql
-- 以下のような検索では効果がない
-- 範囲検索（使用不可）
SELECT * FROM session_data WHERE session_id > 'aaa';

-- 部分一致検索（使用不可）
SELECT * FROM session_data WHERE session_id LIKE 'abc%';

-- ソート（使用不可）
SELECT * FROM session_data ORDER BY session_id;
```

### 全文検索インデックス

テキストデータの検索に特化したインデックスで、転置インデックス構造を使用します。

```sql
-- MySQL での全文検索インデックスの作成
CREATE TABLE articles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255),
    content TEXT,
    FULLTEXT idx_fulltext (title, content)
);

-- 全文検索の実行
SELECT * FROM articles 
WHERE MATCH(title, content) AGAINST('データベース 最適化' IN BOOLEAN MODE);

-- スコア付き検索
SELECT id, title, 
       MATCH(title, content) AGAINST('データベース') AS relevance
FROM articles 
WHERE MATCH(title, content) AGAINST('データベース')
ORDER BY relevance DESC;
```

### インデックスの種類と特徴の比較

![インデックスの種類と特徴](images/index-types-comparison.svg)

## 7.2 インデックス設計の指針

効果的なインデックス設計は、データベースのパフォーマンスに大きく影響します。

### カーディナリティの考慮

カーディナリティ（値の種類の多さ）が高いカラムほど、インデックスの効果が高くなります。

```sql
-- 高カーディナリティ（効果的）
CREATE INDEX idx_user_id ON orders(user_id);      -- ユーザーIDは多様
CREATE INDEX idx_email ON users(email);           -- メールアドレスはユニーク

-- 低カーディナリティ（効果が限定的）
CREATE INDEX idx_gender ON users(gender);         -- 性別は2-3種類のみ
CREATE INDEX idx_status ON orders(status);        -- ステータスは数種類
```

### 複合インデックスの設計

複数のカラムを組み合わせたインデックスは、適切に設計すれば非常に効果的です。

```sql
-- 複合インデックスの作成
CREATE INDEX idx_user_date ON orders(user_id, order_date);

-- この複合インデックスが使用されるクエリ
-- 両方のカラムを使用（最も効率的）
SELECT * FROM orders 
WHERE user_id = 100 AND order_date = '2024-01-01';

-- 左端のカラムのみ使用（インデックス使用可能）
SELECT * FROM orders WHERE user_id = 100;

-- 右端のカラムのみ使用（インデックス使用不可）
SELECT * FROM orders WHERE order_date = '2024-01-01';
```

### カバリングインデックス

クエリで必要なすべてのカラムをインデックスに含めることで、テーブルへのアクセスを避けられます。

```sql
-- カバリングインデックスの例
CREATE INDEX idx_covering ON orders(user_id, order_date, total_amount);

-- このクエリはインデックスのみで完結（高速）
SELECT user_id, order_date, total_amount 
FROM orders 
WHERE user_id = 100;

-- EXPLAINで確認
EXPLAIN SELECT user_id, order_date, total_amount 
FROM orders WHERE user_id = 100;
-- Extra: Using index と表示される
```

### インデックスの選択性

選択性（Selectivity）は、インデックスがどれだけ効率的に行を絞り込めるかを示します。

```sql
-- 選択性の計算
SELECT 
    COUNT(DISTINCT user_id) / COUNT(*) AS user_id_selectivity,
    COUNT(DISTINCT status) / COUNT(*) AS status_selectivity
FROM orders;

-- 選択性が高いカラムを優先してインデックス化
-- user_id_selectivity: 0.95 (高い - 効果的)
-- status_selectivity: 0.003 (低い - 効果限定的)
```

## 7.3 実行計画の読み方

実行計画は、データベースがクエリをどのように処理するかを示すロードマップです。

![クエリ実行計画の例](images/query-execution-plan.svg)

### MySQLでの実行計画の取得

```sql
-- EXPLAINを使用した実行計画の確認
EXPLAIN SELECT o.*, c.name 
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.order_date >= '2024-01-01';

-- 詳細な実行計画（MySQL 5.7以降）
EXPLAIN FORMAT=JSON 
SELECT o.*, c.name 
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.order_date >= '2024-01-01';
```

### 実行計画の重要な項目

#### type（アクセスタイプ）

```sql
-- システムテーブル（最速）
EXPLAIN SELECT * FROM (SELECT 1) AS t;  -- type: system

-- const（プライマリキーまたはユニークキーでの検索）
EXPLAIN SELECT * FROM users WHERE id = 1;  -- type: const

-- eq_ref（結合時のユニークキー検索）
EXPLAIN SELECT * FROM orders o 
JOIN customers c ON o.customer_id = c.id;  -- type: eq_ref

-- ref（非ユニークインデックス検索）
EXPLAIN SELECT * FROM orders WHERE user_id = 100;  -- type: ref

-- range（範囲検索）
EXPLAIN SELECT * FROM orders 
WHERE order_date BETWEEN '2024-01-01' AND '2024-12-31';  -- type: range

-- index（インデックスフルスキャン）
EXPLAIN SELECT user_id FROM orders;  -- type: index

-- ALL（フルテーブルスキャン - 最も遅い）
EXPLAIN SELECT * FROM large_table WHERE some_column = 'value';  -- type: ALL
```

#### Extra（追加情報）

```sql
-- Using index（カバリングインデックス使用）
EXPLAIN SELECT user_id FROM orders WHERE user_id > 100;
-- Extra: Using where; Using index

-- Using filesort（ソート処理が必要）
EXPLAIN SELECT * FROM orders ORDER BY total_amount;
-- Extra: Using filesort

-- Using temporary（一時テーブル使用）
EXPLAIN SELECT DISTINCT user_id FROM orders;
-- Extra: Using temporary

-- Using index condition（インデックスコンディションプッシュダウン）
EXPLAIN SELECT * FROM orders WHERE user_id = 100 AND status = 'completed';
-- Extra: Using index condition
```

### PostgreSQLでの実行計画

```sql
-- EXPLAIN ANALYZEで実際の実行時間も確認
EXPLAIN ANALYZE
SELECT o.*, c.name 
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.order_date >= '2024-01-01';

-- バッファ使用状況も確認
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM large_table WHERE condition = true;
```

## 7.4 クエリチューニングの手法

![クエリ最適化のフロー](images/query-optimization-flow.svg)

### インデックスの活用

```sql
-- 非効率なクエリ（関数使用によりインデックスが使えない）
SELECT * FROM users WHERE YEAR(created_at) = 2024;

-- 改善版（インデックスが使える）
SELECT * FROM users 
WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01';

-- 非効率なクエリ（型変換によりインデックスが使えない）
SELECT * FROM users WHERE user_id = '100';  -- user_idがINT型の場合

-- 改善版（型を合わせる）
SELECT * FROM users WHERE user_id = 100;
```

### JOINの最適化

```sql
-- 非効率なクエリ（大きなテーブルから開始）
SELECT * FROM large_orders lo
JOIN small_customers sc ON lo.customer_id = sc.id
WHERE sc.country = 'Japan';

-- 改善版（小さなテーブルから開始）
SELECT * FROM small_customers sc
JOIN large_orders lo ON sc.id = lo.customer_id
WHERE sc.country = 'Japan';

-- 結合条件にインデックスを作成
CREATE INDEX idx_customer_id ON large_orders(customer_id);
CREATE INDEX idx_country ON small_customers(country);
```

### サブクエリの最適化

```sql
-- 非効率なサブクエリ（相関サブクエリ）
SELECT * FROM orders o
WHERE total_amount > (
    SELECT AVG(total_amount) 
    FROM orders 
    WHERE customer_id = o.customer_id
);

-- 改善版（JOINとウィンドウ関数を使用）
WITH customer_avg AS (
    SELECT customer_id, AVG(total_amount) as avg_amount
    FROM orders
    GROUP BY customer_id
)
SELECT o.* FROM orders o
JOIN customer_avg ca ON o.customer_id = ca.customer_id
WHERE o.total_amount > ca.avg_amount;

-- さらに改善（ウィンドウ関数）
SELECT * FROM (
    SELECT *,
           AVG(total_amount) OVER (PARTITION BY customer_id) as avg_amount
    FROM orders
) t
WHERE total_amount > avg_amount;
```

### LIMIT句の活用

```sql
-- 非効率（全件取得後にアプリケーションで制限）
SELECT * FROM large_table ORDER BY created_at DESC;
-- アプリケーションで最初の10件のみ使用

-- 改善版（データベースで制限）
SELECT * FROM large_table ORDER BY created_at DESC LIMIT 10;

-- ページネーションの最適化
-- 非効率（大きなOFFSET）
SELECT * FROM large_table ORDER BY id LIMIT 10 OFFSET 10000;

-- 改善版（WHERE句で範囲指定）
SELECT * FROM large_table WHERE id > 10000 ORDER BY id LIMIT 10;
```

### 集計クエリの最適化

```sql
-- 非効率（全件集計）
SELECT customer_id, COUNT(*) as order_count
FROM orders
GROUP BY customer_id
HAVING COUNT(*) > 10;

-- 改善版（事前集計テーブルの利用）
CREATE TABLE customer_order_stats AS
SELECT customer_id, COUNT(*) as order_count, 
       SUM(total_amount) as total_spent
FROM orders
GROUP BY customer_id;

CREATE INDEX idx_order_count ON customer_order_stats(order_count);

-- 高速な検索
SELECT * FROM customer_order_stats WHERE order_count > 10;
```

## 7.5 統計情報とオプティマイザ

### 統計情報の重要性

オプティマイザは統計情報を基に最適な実行計画を選択します。

```sql
-- MySQL での統計情報の更新
ANALYZE TABLE orders;
ANALYZE TABLE customers;

-- PostgreSQL での統計情報の更新
ANALYZE orders;
VACUUM ANALYZE customers;

-- 統計情報の確認（MySQL）
SELECT * FROM information_schema.statistics 
WHERE table_schema = 'your_database' AND table_name = 'orders';

-- 統計情報の確認（PostgreSQL）
SELECT schemaname, tablename, attname, n_distinct, correlation
FROM pg_stats
WHERE tablename = 'orders';
```

### ヒストグラムの活用

```sql
-- MySQL 8.0以降でのヒストグラム作成
ANALYZE TABLE orders UPDATE HISTOGRAM ON total_amount, order_date;

-- ヒストグラム情報の確認
SELECT * FROM information_schema.column_statistics
WHERE schema_name = 'your_database' AND table_name = 'orders';
```

### オプティマイザヒント

```sql
-- MySQL でのインデックスヒント
SELECT /*+ INDEX(orders idx_user_date) */ *
FROM orders
WHERE user_id = 100 AND order_date = '2024-01-01';

-- 結合順序の指定
SELECT /*+ LEADING(small_table large_table) */ *
FROM small_table
JOIN large_table ON small_table.id = large_table.foreign_id;

-- PostgreSQL でのプランナー設定
SET enable_seqscan = off;  -- フルテーブルスキャンを無効化
SELECT * FROM orders WHERE user_id = 100;
SET enable_seqscan = on;   -- 設定を戻す
```

### コストベースオプティマイザの調整

```sql
-- MySQL のオプティマイザスイッチ
SET optimizer_switch = 'index_merge=on,index_merge_union=on';

-- コスト係数の調整（PostgreSQL）
-- ランダムアクセスコストの調整
SET random_page_cost = 2.0;  -- SSDの場合は低めに設定

-- シーケンシャルアクセスコストの調整
SET seq_page_cost = 1.0;
```

## まとめ

インデックスとクエリ最適化は、データベースパフォーマンスの要です：

1. **適切なインデックス設計**
   - カーディナリティを考慮
   - 複合インデックスの左端優先原則
   - カバリングインデックスの活用

2. **実行計画の理解**
   - EXPLAINコマンドの活用
   - アクセスタイプの確認
   - 追加情報の解釈

3. **クエリチューニング**
   - インデックスが使える条件の記述
   - 結合順序の最適化
   - サブクエリの書き換え

4. **統計情報の管理**
   - 定期的な統計情報の更新
   - ヒストグラムの活用
   - オプティマイザヒントの適切な使用

5. **継続的な監視と改善**
   - スロークエリログの分析
   - 定期的なパフォーマンステスト
   - インデックスの見直しと再構築

次の章では、トランザクションと同時実行制御について学習します。