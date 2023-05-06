set dotenv-load := true
project := "match"


# Run CLJS tests
cljs-test:
  @npx shadow-cljs compile test


# Run CLJ tests
clj-test focus=':unit' +opts="":
  @clojure -M:test -m kaocha.runner             \
           --reporter kaocha.report/dots        \
           --focus {{ focus }}                  \
           {{ opts }}


release version message:
  @git tag -a {{ version }} -m "{{ message }}"
  @git push --tags
  @echo -n "SHA:"
  @git rev-parse --short {{ version }}^{commit}


help:
  @just --list


# Check for outdated deps
outdated:
  clj -M:outdated


# Start Node runtime so that Calva REPL can connect to it
node:
  node ./target/node.js


# Stop ShadowCljs
stop:
  npx shadow-cljs stop


# Start ShadowCljs
start:
  npx shadow-cljs start
