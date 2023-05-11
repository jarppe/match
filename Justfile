set dotenv-load := true
project := "match"


help:
  @just --list


# Run CLJS tests
cljs-test:
  @npx shadow-cljs compile test


# Run CLJ tests
clj-test focus=':unit' +opts="":
  @clojure -M:test -m kaocha.runner             \
           --reporter kaocha.report/dots        \
           --focus {{ focus }}                  \
           {{ opts }}


# Run both CLJ and CLJS tests
test: clj-test cljs-test
  @echo "All tests passed!"


# Make a release, creates a tag and pushes it
@release version +message:
  git tag -a {{ version }} -m "{{ message }}"
  git push --tags
  bash -c 'echo -n "SHA: "'
  git rev-parse --short {{ version }}^{commit}


current-release:
  #!/usr/bin/env bash
  TAG=$(git tag -l | tail -n 1)
  SHA=$(git rev-parse --short ${TAG}^{commit})
  echo ":git/tag \"${TAG}\""
  echo ":git/sha \"${SHA}\""

# Check for outdated deps
outdated:
  clj -M:outdated


# Start Node runtime so that Calva REPL can connect to it
node:
  #!/usr/bin/env bash
  while :; do 
    echo "Starting NodeJS environment..."
    node ./target/node/index.js
    echo 'Restaring NodeJS environment in 2 seconds...'
    sleep 2
  done
