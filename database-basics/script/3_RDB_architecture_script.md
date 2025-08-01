# 話す原稿：導入・全体像

---

皆さん、こんにちは。  
今日はリレーショナルデータベース、略してRDBの「仕組み」と「構造」についてお話しします。  

まず、なぜこの話をするかというと、  
ただSQLを書くだけでなく、DBの内部がどう動いているかを理解することで、  
例えば「なぜデータベースの動作が遅くなるのか？」や  
「障害が起きたときにどうやって復旧するのか？」といった課題に対して、  
自分で原因を考えられるようになるためです。  

実際、SQLを発行すると、  
裏側では単にデータを取りに行くだけでなく、  
メモリの管理やデータファイル、ログファイルの読み書きなど、  
様々な処理が行われています。  

これらの処理の役割や構造を理解できると、  
パフォーマンスのボトルネックを特定したり、  
トラブルシューティングがぐっと楽になります。  

今日の講義では、RDBがどのような構成で成り立っていて、  
クライアントからのリクエストがどのように処理されるか、  
ざっくりとした全体像を押さえていきましょう。  

これから話す内容は、今後の業務や学習で必ず役に立ちますので、  
ぜひ一緒に理解を深めていきましょう。  

# 話す原稿：クライアント・サーバーモデル

---

次に、「クライアント・サーバーモデル」について説明します。

データベースは、**クライアントとサーバーが通信する構成**で動いています。  
クライアントとはSQLを送る側で、たとえばアプリケーション、バッチ処理、BIツール、シェルスクリプトなどが含まれます。  
一方、サーバー側のRDBMSが、それを受け取り、処理し、結果を返します。

この通信は、通常 **TCP/IP** を使って行われます。  
クライアントから送られたSQLはサーバーで解析・最適化され、実行されて、結果が返ります。

ここで図を見てください。  
![sample](./images/3_01_RDB_architecture_basic.svg)

このように、複数のクライアントが同時に1つのサーバーに接続してくることができます。  
たとえば、APIを発行するWebアプリ、バッチ処理、BIツールなどが同時に動いている状況が考えられます。

このとき重要になるのが「**コネクション管理**」です。  
データベースには、同時に処理できる接続数に上限があります。  
たとえばPostgreSQLなら、デフォルトで100接続などです。  
これを超えると新しい接続は弾かれてしまいます。

そのため、Webアプリケーションでは**接続プール**という仕組みで、  
いちいち接続を張りなおすのではなく、あらかじめ接続を確保して使い回します。  
これによりパフォーマンスが安定します。

この構造を知っておくと、  
SQLが遅いときに「アプリがSQLを送れていないのか？」「DBが詰まっているのか？」といった  
原因の切り分けがしやすくなります。

つまり、このクライアント・サーバー構造の理解は、  
**性能問題・障害対応・設計の判断力を上げる基礎**になるのです。

このあとからは、サーバー側が実際にどう処理を行っているかを  
メモリ・ファイル・プロセスの視点で見ていきます。


# 話す原稿：データベースの物理構造

---

ここからは、データベースの「物理構造」について説明します。

普段、私たちはSQL文だけを触っていますが、  
その裏では、データベースはすべて「ファイル」としてディスク上に存在しています。

その構造を知ることで、  
IOがなぜ発生するのか、ログがどう使われるのか、  
どうしてインデックスが速くなるのか、ということがイメージしやすくなります。

データベースで扱われる代表的なファイルは、次の3種類です。

1つ目が「**データファイル**」。  
これはテーブルの中身、つまり行データそのものが格納されているファイルです。  
多くのRDBMSでは、**ページ単位（例えば8KB）**で読み書きされており、  
Bツリー構造などで管理されています。  
行数が多くなっても効率的にアクセスできるようになっています。

2つ目が「**ログファイル**」、別名「トランザクションログ」です。  
このファイルには、データの更新履歴が記録されています。  
何のためにあるかというと、例えば**クラッシュした時の復旧**に使います。  
Write-Ahead Logging（WAL）という仕組みで、  
更新はまずログファイルに書き込んでから、本体のデータファイルに反映します。

3つ目が「**インデックスファイル**」です。  
これは検索を高速化するためのファイルです。  
テーブルのデータとは別に保存されていて、B+ツリーやハッシュ構造が使われます。  
SQLでWHERE句やJOINが速くなるのは、主にこのインデックスのおかげです。

ここで、ファイル構造の図を見てみましょう。  
![sample](./images/3_02_RDB_file_structure.svg)

このように、RDBMSの物理的な構成は、  
「データファイル」「ログファイル」「インデックスファイル」といった複数の役割を持つファイルで成り立っています。

この構造を理解しておくことで、  
- ストレージをどれくらい消費するのか  
- インデックスが効かないときにどこを疑えばいいのか  
- トラブル時にどのログを見ればいいのか  

といった実践的な判断ができるようになります。

次は、このファイル構造と深く関係する「メモリとバッファ管理」について見ていきます。

# 話す原稿：メモリ構造とバッファ管理

---

続いて、RDBMSの「メモリ構造とバッファ管理」について説明します。

データベースの処理において最も重たいのは、ディスクへのアクセスです。  
ディスクはメモリに比べて圧倒的に遅いため、  
できるだけ**メモリ上で処理を完結させる**ことがパフォーマンスの鍵になります。

そのために、RDBMSは**独自のバッファ管理機構**を持っていて、  
代表的なものが「バッファプール（Buffer Pool）」です。

バッファプールは、**ディスク上のデータページをキャッシュする場所**です。  
SQLで何かのテーブルにアクセスしたとき、まずはこのバッファにデータがあるかどうかを確認します。  
あれば即座に処理を実行でき、なければディスクから読み込みます。  
これを「**バッファヒット**」といい、ヒット率が高いほど高速に動作します。

書き込み時もすぐにディスクには書かず、一度バッファプールに変更を加え、  
その後まとめてディスクに書き出します。これが**ダーティページのフラッシュ**です。

次に、「**ログバッファ**」という領域もあります。  
これはトランザクションログを書き込む前に、一時的に保持する場所です。  
WAL（Write Ahead Logging）の仕組みにより、ログが先にバッファに入り、  
後からディスクにフラッシュされます。これによりクラッシュ時の復旧が可能になります。

さらに、ORDER BY や GROUP BY などの操作を行うときには、  
「**一時的な作業領域**」が使われます。  
この領域が不足すると、一時ファイルがディスクに作られ、処理が大幅に遅くなります。  
それを防ぐにはメモリサイズの適切な設定が重要です。

ここで図を見てください。  
![sample](./images/3_03_RDB_memory_buffer.svg)

このように、RDBMSのメモリ領域は、ディスクと密接に連携しながら処理の効率化を行っています。  
バッファがデータとログの両方の橋渡しをしているのがポイントです。

この構造を知っておくと、以下のような観点でのチューニングが可能になります。

- クエリ実行が遅いとき、I/Oが原因なのかメモリが原因なのかの切り分け  
- キャッシュヒット率を上げるための設定調整  
- 作業領域サイズを増やしてソート処理を高速化

このように、RDBMSの性能はメモリの使い方で大きく左右されます。  
次はこのメモリ管理と密接に関係する「プロセス構造と接続管理」について見ていきましょう。

# 話す原稿：プロセス構造と接続管理

---

次に、「プロセス構造と接続管理」について説明します。

これまで見てきたように、RDBMSはクライアントからのリクエストを処理するサーバーですが、  
その中身は実際には**複数のプロセスやスレッド**で構成されています。

たとえばPostgreSQLはマルチプロセス型で、  
1つの接続ごとに1つのプロセスが生成されます。  
MySQLはマルチスレッド型で、スレッドで処理が分担されます。  
DBのアーキテクチャによって処理の並列性や消費リソースが異なります。

プロセスは主に以下のような役割を持っています。

1つ目が「**クライアント接続プロセス（またはスレッド）**」です。  
クライアントからSQLが届いたとき、このプロセスがSQLの解析・実行・結果返却までを行います。  
基本的に、1つの接続に対して1つのプロセス（またはスレッド）が割り当てられます。

2つ目が「**バックグラウンドプロセス**」。  
チェックポイントの作成や、ログの書き込み、不要データの削除（バキューム）など、  
日々のメンテナンスや安定動作のために働いているプロセスです。  
これらは非同期に動き、パフォーマンスと整合性を維持します。

3つ目が「**I/O専用プロセス**」です。  
バッファからディスクへデータを書き出したり、必要なデータを読み込んだりする役割があります。  
これにより、SQL実行プロセスがI/O待ちで止まらないように設計されています。

ここで図を見てください。  
![sample](./images/3_04_RDB_process_connection.svg)

接続が1つ増えるごとに、プロセスやスレッドが生成され、  
CPUやメモリ、ファイルディスクリプタなどのリソースを消費します。

そのため、接続数が増えすぎると、サーバーが重くなったり、  
新しい接続ができずにアプリケーションエラーが出ることがあります。

これを防ぐために、**Webアプリケーションでは接続プール**を使うのが一般的です。  
使わなくなった接続はすぐに破棄せず、プールに戻して再利用することで、  
接続数の増加を抑えつつ、レスポンスも早くなります。

このプロセスと接続の仕組みを理解していると、  
・パフォーマンスが落ちているときに、接続が詰まっていないか？  
・バックグラウンド処理が遅れていないか？  
といった視点で調査できるようになります。

このあとは、データを実際に処理する「ストレージエンジンの仕組み」を見ていきましょう。

# 話す原稿：ストレージエンジンの仕組み

---

最後に、RDBMSの「ストレージエンジンの仕組み」について説明します。

ここまで説明してきたファイルやメモリ、プロセスの裏側で、  
それらを実際に**制御・操作している中核モジュール**がストレージエンジンです。

ストレージエンジンは、  
- テーブルやインデックスをどのような構造で保存するか  
- データをいつどのようにディスクに書き込むか  
- トランザクションの整合性をどう保つか  
などを担っていて、まさに**RDBMSの心臓部**とも言える存在です。

たとえば、MySQLでは複数のストレージエンジンを切り替えることができます。  
代表的なのが「InnoDB」「MyISAM」「MEMORY」の3つです。

- **InnoDB** は、MySQLのデフォルトエンジンで、ACIDに準拠しておりトランザクションや外部キーをサポートしています。  
- **MyISAM** は古いエンジンで、軽量で高速な読み取りに強いですが、トランザクションに対応していません。  
- **MEMORY** エンジンは、その名の通り、データをメモリ上に一時的に保存するもので、超高速ですが永続性がありません。

用途に応じてこれらを使い分けることで、処理の最適化が可能です。

ここで図を見てください。  
![sample](./images/3_05_RDB_storage_engine.svg)

図のように、ストレージエンジンはSQL文の処理結果を受け取り、  
メモリやファイル、ログとやり取りしながら、実際のデータ操作を行います。

ストレージエンジンによって、  
- どんなインデックスが使えるか  
- トランザクションが使えるか  
- クラッシュ復旧に強いかどうか  
といったポイントがすべて変わってきます。

そのため、例えば業務システムではInnoDB、  
キャッシュ的な使い方にはMEMORY、  
ログ的な用途でMyISAMなど、**適材適所の選定**が求められます。

また、他のRDBMS（PostgreSQLやOracleなど）では、  
ストレージエンジンは固定ですが、その内部構造は共通する考え方を持っています。

この仕組みを知っておくことで、  
- データの永続性がどこで担保されているか  
- なぜクラッシュしてもロールバックできるのか  
- なぜインデックスが速く効くのか  
といった疑問の理解が深まります。

これで、RDBMSの仕組みと構造の全体をひととおりカバーできました。
