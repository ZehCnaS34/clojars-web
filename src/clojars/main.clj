(ns clojars.main
  (:gen-class)
  (:require
   [clojars
    [admin :as admin]
    [config :as config]
    [errors :as err]
    [system :as system]]
   [com.stuartsierra.component :as component]
   [meta-merge.core :refer [meta-merge]]
   [raven-clj.core :as raven-clj]))

(def prod-env
  {:app {:middleware []}})

(defn info [& msg]
  (apply println "clojars-web:" msg))

(defn warn [& msg]
  (apply println "clojars-web: WARNING -" msg))

(defn prod-system [config prod-reporter]
  (-> (meta-merge config prod-env)
      system/new-system
      (assoc
       :error-reporter (err/multiple-reporters
                        (err/log-reporter)
                        prod-reporter))))

(defn error-reporter [config]
  (let [dsn (:sentry-dsn config)]
    (if (and dsn (not= "NOTSET" dsn))
      (let [raven-reporter (err/raven-error-reporter {:dsn dsn})]
        (info "enabling raven-clj client dsn:project-id:" (:project-id (raven-clj/parse-dsn dsn)))
        (Thread/setDefaultUncaughtExceptionHandler raven-reporter)
        raven-reporter)
      (do
        (warn "no :sentry-dsn set in config, errors won't be logged to Sentry")
        (err/null-reporter)))))

(defn -main [& _args]
  (try
    (alter-var-root #'config/*profile* (constantly "production"))
    (let [config (config/config)
          system (component/start (prod-system config (error-reporter config)))]
      (info "starting jetty on" (str "http://" (:bind config) ":" (:port config)))
      (admin/init (get-in system [:db :spec])
                  (:search system)
                  (:storage system)))
    (catch Throwable t
      (binding [*out* *err*]
        (println "Error during app startup:"))
      (.printStackTrace t))))
