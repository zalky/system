(ns system.repl
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.track :as track]
            [system.reload :as reload]
            [clojure.main :as main]
            [clojure.stacktrace :as st]
            [io.aviso.ansi :refer [bold-red bold-yellow]] ))


(declare system)
(declare system-sym)

(defn set-init! [sys]
  (intern 'system.repl 'system-sym (symbol (str (:ns (meta sys))) (str (:name (meta sys))))))

(defn init
  "Constructs the current development system."
  []  
  (alter-var-root #'system (constantly ((find-var system-sym)))))

(defn start
  "Starts the current development system if not already started."
  []
  (when-not (::started system)
    (init)
    (alter-var-root #'system
      (fn [s]
        (some-> s
                component/start
                (assoc ::started true))))))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
    (fn [s]
      (some-> s
              component/stop
              (dissoc ::started)))))

(defn reset []
  (stop)
  (start))

(defn refresh [tracker {:keys [restart? mode]}]
  (when restart?
    (stop)
    (println (bold-yellow (str "Stopping " system-sym))))

  (when (= mode :tools.namespace) (println "Unmapping namespaces:" (::track/unload @tracker)))
  (println "Recompiling namespaces:" (::track/load @tracker))
  (swap! tracker reload/track-reload (= mode :tools.namespace))
  (when-let [e (::reload/error @tracker)]
    (swap! @(resolve 'boot.core/*warnings*) inc)
    (println (bold-red (str "Error reloading: " (::reload/error-ns @tracker))))
    (binding [*out* *err*]
      (-> e
          Throwable->map
          main/ex-triage
          main/ex-str
          println)))

  (when restart?
    (start)
    (println (bold-yellow (str "Starting " system-sym)))))


;; No need to break API signatures

(def go start)

