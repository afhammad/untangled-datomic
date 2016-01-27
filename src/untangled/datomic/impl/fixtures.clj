(ns untangled.datomic.impl.fixtures
  (:require
    [datomic.api :as d]
    [com.stuartsierra.component :as component]
    [untangled.datomic.protocols :as udb]
    [untangled.datomic.core :refer [build-database]]
    [taoensso.timbre :as t]))

(defn db-fixture
  "Create a test fixture version (in-memory database) of a database. Such a
  database will be auto-migrated to the current version of the schema in the
  given (optional) namespace, and the
  given seed-fn will be run as part of the database startup (see the database
  component in datahub for details on seeding).
  "
  [db-key & {:keys [migration-ns seed-fn log-level]}]
  (let [
        uri "datomic:mem://db-fixture"
        db  (build-database db-key)]
    (d/delete-database uri)
    (t/set-level! log-level)
    (component/start (assoc db :config {:value {:datomic {:dbs {db-key
                                                                (cond-> {:url uri :auto-drop true}
                                                                  migration-ns (assoc :auto-migrate true :schema migration-ns)
                                                                  seed-fn (assoc :seed-function seed-fn))}}}}))))

(defmacro with-db-fixture
  "
  Set up the specified database, seed it using seed-fn (see Database component),
  make it available as varname, run the given form, then clean up the database.

  Returns the result of the form.
  "
  [varname form & {:keys [migrations seed-fn log-level] :or {log-level :fatal}}]
  `(let [~varname (db-fixture :mockdb :migration-ns ~migrations :seed-fn ~seed-fn :log-level ~log-level)]
     (try ~form (finally
                  (component/stop ~varname)
                  (t/set-level! :debug))))
  )


