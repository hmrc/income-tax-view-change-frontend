#!/usr/bin/env bash

set -o pipefail
set -u

MAX_ITERATIONS=100

TEST_CLASSES=(
  "controllers.agent.EnterClientsUTRControllerISpec"
  "controllers.WhatYouOweControllerISpec"
  "controllers.ChargeSummaryControllerSpec"
)

BASE_LOG_DIR="flaky-test-logs"
DATE_DIR=$(date +"%Y%m%d_%H%M%S")
LOG_DIR="$BASE_LOG_DIR/$DATE_DIR"

mkdir -p "$LOG_DIR"

echo "========================================"
echo "Flaky test runner started"
echo "Iterations per test: $MAX_ITERATIONS"
echo "Logs directory: $LOG_DIR"
echo "Tests:"
printf '  - %s\n' "${TEST_CLASSES[@]}"
echo "========================================"
echo

for TEST_CLASS in "${TEST_CLASSES[@]}"; do
  echo "========================================"
  echo "Running test: $TEST_CLASS"
  echo "========================================"
  echo

  for ((ITERATION=1; ITERATION<=MAX_ITERATIONS; ITERATION++)); do
    echo "----------------------------------------"
    echo "Run #$ITERATION / $MAX_ITERATIONS"
    echo "Test: $TEST_CLASS"
    echo "Started at $(date)"
    echo "----------------------------------------"

    LOG_FILE="$LOG_DIR/${TEST_CLASS}_run_${ITERATION}.log"

    sbt -mem 5555 "it:testOnly $TEST_CLASS" \
      2>&1 | tee "$LOG_FILE"

    SBT_EXIT_CODE=${PIPESTATUS[0]}

    if [[ $SBT_EXIT_CODE -ne 0 ]]; then
      echo
      echo "FAILURE detected"
      echo "Test: $TEST_CLASS"
      echo "Iteration: $ITERATION"
