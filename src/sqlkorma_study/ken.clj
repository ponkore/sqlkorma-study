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
