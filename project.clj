(defproject wfh "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.6.1"]
                 [korma "0.4.0"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
                 [mysql/mysql-connector-java "5.1.38"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [hiccup "1.0.4"]
                 [clj-time "0.11.0"]
                 [dire "0.5.3"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler wfh.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
