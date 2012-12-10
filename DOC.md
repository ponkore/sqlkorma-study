sqlkorma tutrial
================

この記事は、[Lispアドベントカレンダー2012](http://qiita.com/advent-calendar/2012/lisp) の 11日目 の記事です。前日は、nitro_idiot さんがライブコーディングで凄まじい勢いでWebサイトを作る [記事](http://d.hatena.ne.jp/nitro_idiot/20121210/1355118962) でした。

今日のネタは、いろいろ考えた挙句 [sqlkorma](http://sqlkorma.com/) にしました。

動機
----

自分はいわゆる SE という仕事をしておりまして、これまたいわゆる **業務システム** というやつを相手に商売をしております。「いわゆる業務システムって何やねん」といわれるとこういったオープンな場では詳細には説明できないのですが、ざっくりいうと Non-Web 系のフルスクラッチなものが多いです。ぶっちゃけレガシーな技術を適用した大規模なウォーターフォールな開発がメインだったりするのですが、個人的には最近の関数型言語ブーム(？)に乗っかってみたいと思ってたりします。

実際のところ、「自分の開発現場で関数型言語導入」とか、まだそんな時期ではなかったりするのですが、自分の「腕を磨く」という意味で「職場で何か」手を動かし続ける、ということには意味があると思っています。

職場では、いわゆる **Excel 方眼紙の仕様書** を相手にすることが多く、これはこれで悩みの種ではあるのですが、こちらについては [別途](https://gist.github.com/4216377) アプローチするとして、一方で **RDB** を相手に設計やらデータ調査やらすることも多く、これはこれで悩みの種だったりします。

**SQL** をその場その場で必要に応じて書くのですが、出力結果を Excel で見やすい形に整形してコメントをつけて文書化、といった作業が延々と続きます。ツールとしては市販のツールを幾つか使っているのですが、やはり **若干の手作業** は残ってしまいます。これをどうにかするには、必要に応じて柔軟にカスタマイズできるオレオレツールを作るのが最善の策です。ここでいう「最善の」は「俺得」という意味です。会社はこういう場合、通常「俺得ツール」を作るコストなんか許してくれません。それでも「サクッと」作れてしまえるのなら、なんらかの作業の合間にねじ込んでしまえるというもの。

若干こじつけになってしまいましたが、

* RDB を操作する仕事を省力化したい
* いわゆる「俺得」ツールを「サクッと」作れるようにしたい

というのが、今回の動機になります。

sqlkorma とは
------------

[sqlkorma](http://sqlkorma.com/) とは、Clojure から RDB へアクセスするための JDBC のラッパーの「ちょい上」のような位置づけのライブラリです。見ようによっては DSL といえなくもないのですが、個人的にはなんとなくライブラリといったほうがしっくりきます。作者は、あの超オシャレ Clojure IDE [Light Table](http://www.lighttable.com/) とか、Clojure Web Framework [noir](http://www.webnoir.org/) の [Chris Granger さん](http://www.chris-granger.com/)です。

準備する環境
----------

**sqlkorma** を試すには、`leiningen` とあと *RDB* があれば試すことができます。 *RDB* は、

* mssql
* mysql
* oracle
* postgresql
* sqlite3

あたりが良さげです(接続用の関数が定義されています)。

例えば、postgresql だと [ここ](http://sqlkorma.com/docs) にもろにサンプルとして
```clojure
(defdb prod (postgres { ...
                             }))
```
というように `postgres` という接続用の関数の用例が載っておりますのですぐ使えるでしょう。

実は会社では `Oracle` を使うことが多いのですが、`Oracle` を使う場合にはちょっとした工夫が必要です。
Oracle 用関数 `oracle` には、オプションとして `:host`、`:port` を渡すことはできても接続先サービス名(データベースインスタンス名のようなもの)を指定することができません。ところが、内部的には、`:subname` というオプションを`:host`,`:port`から内部で生成しているらしく、これを上書いてやることで目的を達成することができます。具体的には、

```clojure
(defdb db (oracle {:subname ":HOSTNAME-or-IP:PORT:SERVICE"}))
```

こんな感じです。

とここまで postgresql だの Oracle だの言っておきながら、以下の文面では `sqlite3` を使うことにします。`sqlite3` だと別途サーバ環境を準備する必要がないのでラクですよね。

まずは、簡単なサンプルから。サンプル用のスキーマは、以下の様な感じにします。

```SQL
create table email (
  id int primary key,
  address varchar(100)
);
create table users (
  id int primary key,
  first_name varchar(20),
  last_name varchar(20),
  email_id int
);
```
(※sqlite3 なのに型なんて...とかいうツッコミはなしということで。一応上記SQLは postgresql でも動くようにしました。)
上記テーブルを sqlite3 コマンドを使って事前に作っておきます。

    bash-3.2$ sqlite3 sqlkorma-study.db
    sqlite3 sqlkorma-study.db
    SQLite version 3.7.14.1 2012-10-04 19:37:12
    Enter ".help" for instructions
    Enter SQL statements terminated with a ";"
    sqlite> .read create.sql
    .read create.sql
    sqlite> .schema
    .schema
    CREATE TABLE email (
      id int primary key,
      addr varchar(100)
    );
    CREATE TABLE users (
      id int primary key,
      first varchar(20),
      last varchar(20),
      email_id int
    );
    sqlite> .quit
    .quit
    bash-3.2$

leiningen project の作成
-----------------------

ここでお決まりの *leiningen* に登場してもらいます。

```shell
$ lein new sqlkorma-study
$ cd sqlkorma-study
$  # ここで project.clj を編集
$ lein deps
```

`project.clj` は、以下のように `:dependencies` に

* korma
* log4j
* sqlite-jdbc

を追加します。

```clojure
(defproject sqlkorma-study "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[korma "0.3.0-beta9"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [org.clojure/clojure "1.4.0"]])
```

(sqlite は native アクセスが必要ですが、sqlite-jdbc の中に native API の shared library も含まれますので、leiningen がよきに計らってくれます)

プログラミング(初歩の初歩)
----------------------

最初は core.clj を作りながら nrepl で動かしつつ試していきます。`ns`とか`require`は、最低限下記のものが必要です

```clojure
(ns sqlkorma-study.core
  (:require [korma.db :as db]
            [korma.core :as k]))
```

:as の db とか k とかは単に個人的趣味です。korma.core は :as しないほうが良いかもしれません(頻繁に使うので)。

最初にやることは、データベースに接続することです。

```clojure
(db/defdb db (db/sqlite3 {:db "sqlkorma-study.db"}))
;=> {:pool #<Delay@519f6780: :pending>, :options {:naming {:keys #<core$identity clojure.core$identity@43ff887>, :fields #<core$identity clojure.core$identity@43ff887>}, :delimiters ["\"" "\""]}}
```

テーブルに簡単にアクセスするには、テーブルに対応した entity を作成することです。email および users に対する entity 定義を以下のようにします。

```clojure
(k/defentity email
  (k/pk :id)
  (k/entity-fields :email_addr))

(k/defentity users
  (k/pk :id)
  (k/entity-fields :first :last)
  (k/has-one email))
```

次にデータを入れていきます。データは、`korma.core/insert` というものを使います。値は、`korma.core/values` という関数を経由する形で hash-map で渡してあげます。

```clojure
(k/insert users (k/values {:id 1 :first_name "Taro" :last_name "Hoge"}))
(k/insert users (k/values {:id 2 :first_name "じろう" :last_name "Hoge"}))

(k/insert email (k/values {:id 0 :address "example-taro@hoge.com" :users_id 1}))
(k/insert email (k/values {:id 1 :address "example2-taro@fuga.com" :users_id 1}))

(k/insert email (k/values {:id 3 :address "example-jiro@hoge.com" :users_id 2}))
(k/insert email (k/values {:id 4 :address "example-jiro2@fuga.com" :users_id 2}))
(k/insert email (k/values {:id 5 :address "example2-jiro@fuga.com" :users_id 2}))
```

あとは、テーブル単体でデータが欲しければ、

```clojrue
(k/select users)
=> [{:id 1, :first_name "Taro", :last_name "Hoge"} {:id 2, :first_name "じろう", :last_name "Hoge"}]
```

とやれば得られますし、user に紐付く email の情報もあわせて取得したい場合(join して取ってきたい場合)は、

```clojure
(clojure.pprint/pprint (k/select users (k/with email)))
;=> [{:id 1,
  :first_name "Taro",
  :last_name "Hoge",
  :id_2 0,
  :address "example-taro@hoge.com",
  :users_id 1}
 {:id 1,
  :first_name "Taro",
  :last_name "Hoge",
  :id_2 1,
  :address "example2-taro@fuga.com",
  :users_id 1}
 {:id 2,
  :first_name "じろう",
  :last_name "Hoge",
  :id_2 3,
  :address "example-jiro@hoge.com",
  :users_id 2}
 {:id 2,
  :first_name "じろう",
  :last_name "Hoge",
  :id_2 4,
  :address "example-jiro2@fuga.com",
  :users_id 2}
 {:id 2,
  :first_name "じろう",
  :last_name "Hoge",
  :id_2 5,
  :address "example2-jiro@fuga.com",
  :users_id 2}]
nil
```
のようにやれば取得できます。

どんな SQL が発行されているかは、`korma.core/sql-only` を使います。
```clojure
(k/sql-only (k/select users (k/with email)))
;=> "SELECT \"users\".*, \"email\".* FROM \"users\" LEFT JOIN \"email\" ON \"users\".\"id\" = \"email\".\"users_id\""
```

実は、注目すべき点は、korma の select が返す「行」は、clojure の hash-map だということです。この性質により、うまくプログラムの構造を作れば、DB アクセス層を完全に独立させ、ビジネスロジック層を korma とは独立した状態にすることができます。まあうまくやれば、の話ですが。


ちょっとだけ応用編
---------------

これだとあまりにもデータが少なく、実感がわかないので、世の中に出回っている何かのデータを取り込んでみたいと思います。ここでは、[統計局ホームページ](http://www.stat.go.jp/) というところから、地域コードのデータ(CSV形式)を引っ張ってきて、SQLite に放り込んでみる、ということをやってみます。

... こまかな解説は省略しますが、以下の点がポイントになります。

* 元データは URL で直接ダウンロードできるようになっているので、clojure で直接アクセスする。
* ただし、元データの文字コードが SJIS なので、エンコーディングを指定できるよう、clojure.java.io/reader を使う。
* １行目が項目名なのでそこは読み飛ばす。
* 改行は CR-LF、カンマ区切り、なので適当に正規表現で切り取る(すんません、ちょっと雑な実装です)
* `korma.core/insert` 関数で DB に１レコードずつ投入する。そのときCSV１レコードのデータを各項目毎のキーを持つhash-mapに変換して `korma.core/values` に渡してやる。

といったところでしょうか。ソースは以下のようになりました。

```clojure
;;; src/korma_study/ken.clj
(ns sqlkorma-study.ken
  (:require [korma.db :as db]
            [korma.core :as k]))

(db/defdb db (db/sqlite3 {:db "sqlkorma-study.db"}))

(def ken-sityouson-csv-url
  "統計局ホームページ-統計に用いる標準地域コードの取得元URL"
  "http://www.stat.go.jp/index/seido/csv/9-5.csv")

#_"create table ken_sityouson_table (
  ken_code int,
  sityouson_code int,
  tiiki_code int,
  ken_name text,
  sityouson_name1 text,
  sityouson_name2 text,
  sityouson_name3 text,
  yomigana text
);"

(def ken_sityouson_fields
  "ken_sityouson_table のフィールド(select時に取得する対象とするフィールド)"
  [:ken_code
   :sityouson_code
   :tiiki_code
   :ken_name
   :sityouson_name1
   :sityouson_name2
   :sityouson_name3
   :yomigana])

(k/defentity ken_sityouson_table
  (k/pk :tiiki_code))

(defn read-ken-sityouson-as-vector
  "統計局ホームページ-統計に用いる標準地域コードを読み込んで、改行で区切ってvectorに入れて返す。
元データは Shift_JIS なので、UTF-8 に変換して読み取る。
１行めは項目名なので読み飛ばす。"
  []
  (with-open [rdr (clojure.java.io/reader ken-sityouson-csv-url :encoding "Shift_JIS")]
    (->> (slurp rdr)
         (re-seq #"([^\r\n]+)\r\n")
         (map second)
         (drop 1))))

(defn split-with-comma
  "１行の文字列をカンマで区切る。"
  [line]
  (->> line
       (re-seq #"([^,]*),?")
       (map second)
       (drop-last)))

;(split-with-comma "1,100,1100,北海道,札幌市,,,さっぽろし")
;;=> ("1" "100" "1100" "北海道" "札幌市" "" "" "さっぽろし")

(defn transform-for-insert
  "read-ken-sityouson-as-vector で読み取った各要素を、DB に入れる形式(hash-map)に変換する。"
  [val]
  (reduce merge (map (fn [k v] {k v}) ken_sityouson_fields val)))

(defn insert-all-sityouson-data-to-db
  "統計局ホームページ-統計に用いる標準地域コードのデータを、DBに突っ込む。"
  []
  (doseq [line (read-ken-sityouson-as-vector)]
    (let [arg (transform-for-insert (split-with-comma line))]
      (k/insert ken_sityouson_table (k/values arg)))))

;(insert-all-sityouson-data-to-db)
;;=> nil

(def all (k/select ken_sityouson_table))
;(first all)
;;=>{:ken_code 1, :sityouson_code 0, :tiiki_code 1000, :ken_name "北海道", :sityouson_name1 "", :sityouson_name2 "", :sityouson_name3 "", :yomigana "ほっかいどう"}
```

所感
---
いろいろ触ってみましたが、やはりリレーションを手で記述するのは面倒に思います。ただ逆に言えば、Clojure のプログラムとして自分の「引き出し」にしまっておけば、あとは何度でも応用が効くので、頑張って作業する価値があるかもしれません。複雑な SQL って使い回ししにくいケースがほとんどですので、sqlkorma のようなアプローチもありかもしれません(正直なところ、本格的に業務で使ってないのでまだなんとも言えません...)。

Oracle について言えば、

* 接続文字列がちょっとめんどくさい
* `korma.core/limit`、`korma.core/offset` は Oracle では使えない(Oracle の SQLがサポートしていない)
* より複雑な SQL (副問い合わせ、from 句問い合わせ... Oracle のややこしいSQLたち) はどうすればよいのか？

といった点がひっかかります。

あと、DDL のサポートがよくわかりませんでした(動きません、なのか、自分のやり方がまずいのか...)。今後調べるつもりではありますが、どうせやるなら、Ruby on Rails の ActiveRecord の migration みたいに、migration 用 DSL みたいなものが作れたら、オレオレツールとしては使いみちも広がってくると思っていますのでいつかは作ってみたいです。

なんだか Lisp アドベントカレンダーなのにほとんど SQL の話のようになってしまいました(しかも Clojure 限定...さらにいうと内容薄いorz)。まあそれでも誰かの役に立つことがあれば嬉しいのですが...。

今回作成したソース一式は、[GitHub](https://github.com/ponkore/sqlkorma-study/) においてあります。

明日は **omasanori** さんの記事になります。
