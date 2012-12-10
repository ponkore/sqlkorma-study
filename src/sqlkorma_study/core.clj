(ns sqlkorma-study.core
  (:require [korma.db :as db]
            [korma.core :as k]))

(db/defdb db (db/sqlite3 {:db "sqlkorma-study.db"}))

(k/defentity email
  (k/pk :id)
  (k/entity-fields :email_addr))

(k/defentity users
  (k/pk :id)
  (k/entity-fields :first :last)
  (k/has-one email))

(k/insert users (k/values {:id 1 :first_name "Taro" :last_name "Hoge"}))
(k/insert users (k/values {:id 2 :first_name "じろう" :last_name "Hoge"}))

(k/insert email (k/values {:id 0 :address "example@hoge.com" :users_id 0}))
(k/insert email (k/values {:id 1 :address "example2@fuga.com" :users_id 0}))

(k/insert email (k/values {:id 3 :address "example@hoge.com" :users_id 1}))
(k/insert email (k/values {:id 4 :address "example2@fuga.com" :users_id 1}))
(k/insert email (k/values {:id 5 :address "example2@fuga.com" :users_id 1}))

(k/select users)
(k/select users (k/with email))
(k/select email)
