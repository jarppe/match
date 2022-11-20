set dotenv-load := true
project := "match"

help:
  @just --list


# Start Node runtime so that Calva REPL can connect to it
node:
  node -e 'require("./target")'


# Run CLJS tests
test-cljs:
  echo "Running CLJS tests..."
  npx shadow-cljs compile :test


# Run CLJ tests
test-clj +focus=':unit':
  @clear
  @echo "Running CLJ tests..."
  @clojure -M:test -m kaocha.runner       \
           --reporter kaocha.report/dots  \
           --focus {{focus}}


# Run and watch CLJ tests
test-clj-watch +focus=':unit':
  @clear
  @echo "Running CLJ tests..."
  @clojure -M:test -m kaocha.runner       \
           --reporter kaocha.report/dots  \
           --focus {{focus}}              \
           --watch


# Stop ShadowCljs
stop:
  npx shadow-cljs stop


# Start ShadowCljs
start:
  npx shadow-cljs start
