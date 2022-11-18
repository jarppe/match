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
test-clj:
  echo "Running CLJ tests..."
  echo "TODO: Must move deps from shadow-cljs to deps"
