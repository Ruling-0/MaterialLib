#!/usr/bin/env bash
# CI gate for the Postea migration pipeline: three HorizonQA CI boots of a GTNH pack server prove that
# chunk, player, and custom-storage data written under an old material id list version is remapped exactly
# once to the current one. Requires a pack server directory (server.properties, java9args.txt,
# lwjgl3ify-forgePatches.jar, an accepted eula) with HorizonQA and the mods under test installed, and java
# on PATH; point POSTEA_QA_SERVER at it or pass --server <dir>.
#
#   run.sh all                  the whole gate: fresh v1 world -> seed old data -> shift boot -> idempotence
#                               boot, failing on the first broken assertion; this is the CI entry point
#   run.sh <phase>              one phase for hand-driving: world <name> | examples <true|false> | boot |
#                               discover | seed <itemName> | assert-shift | assert-idempotent
#
# The in-session assertions live in MaterialLib's game-test batch materiallib.postea; a boot log must show
# its "verified <storage>" lines, so a run whose seeds never took effect fails instead of passing vacuously.
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SERVER="${POSTEA_QA_SERVER:-}"
if [[ "${1:-}" == "--server" ]]; then
    SERVER="$2"
    shift 2
fi
[[ -n "$SERVER" && -d "$SERVER" ]] || {
    echo "POSTEA_QA_SERVER (or --server <dir>) must name the pack server directory" >&2
    exit 1
}
SERVER="$(cd "$SERVER" && pwd)"

WORLD_NAME=PosteaQA
WORLD="$SERVER/$WORLD_NAME"
LOGS="$SERVER/postea-qa-logs"
WITNESS_ITEM="${POSTEA_QA_ITEM:-materiallib:ingot}"
STORAGES=(backpack "enderstorage:global" "openblocks:inventory" "gregtech:linkedInputBusses" chunk player)

set_world() {
    sed -i "s/^level-name=.*/level-name=$1/" "$SERVER/server.properties"
}

set_examples() {
    sed -i "s/B:registerExamples=.*/B:registerExamples=$1/" "$SERVER/config/materiallib.cfg"
}

boot() {
    local log="$LOGS/boot-$1.log"
    mkdir -p "$LOGS"
    # A crashed boot writes no report; a leftover one from an earlier run must not pass for it.
    rm -f "$SERVER/horizonqa-result.json"
    (
        cd "$SERVER"
        timeout 2400 java -Xms6G -Xmx6G -Dfml.readTimeout=180 -Dfml.queryResult=confirm @java9args.txt \
            -Dhorizonqa.mode=ci -jar lwjgl3ify-forgePatches.jar nogui < /dev/null > "$log" 2>&1
    )
    local code=$?
    echo "boot $1: server exited $code ($log)"
    # The report path travels as an argument: Git Bash converts argument paths for a native python, but
    # never paths embedded in the -c string.
    [[ -f "$SERVER/horizonqa-result.json" ]] || {
        echo "boot $1: no HorizonQA report (server crashed?); see $log" >&2
        return 1
    }
    python -c "
import json, sys
d = json.load(open(sys.argv[1]))
c = d['counts']
print(sys.argv[2] + ':', d['status'], 'selected', c['selectedTests'], 'passed', c['passed'], 'failed', c['failed'])
sys.exit(0 if d['status'] == 'passed' and c['failed'] == 0 else 1)" "$SERVER/horizonqa-result.json" "boot $1"
}

require_verified() {
    local log="$LOGS/boot-$1.log" missing=0
    for storage in "${STORAGES[@]}"; do
        if ! grep -q "\[materiallib\.postea\] verified $storage" "$log"; then
            echo "boot $1: no in-session verification of $storage (vacuous or failed)" >&2
            missing=1
        fi
    done
    return $missing
}

case "${1:?usage: run.sh [--server <dir>] <all|world|examples|boot|discover|seed|assert-shift|assert-idempotent>}" in
    all)
        original_world="$(grep '^level-name=' "$SERVER/server.properties" | cut -d= -f2)"
        restore() {
            set_world "$original_world"
            set_examples false
        }
        trap restore EXIT

        set_world "$WORLD_NAME"
        set_examples false
        rm -rf "$WORLD"

        boot 1 || exit 1
        grep -q "\[materiallib\.postea\] seeded chunk witnesses" "$LOGS/boot-1.log" || {
            echo "boot 1 did not seed the chunk witnesses" >&2
            exit 1
        }
        python "$HERE/seed.py" seed "$WORLD" "$WITNESS_ITEM" || exit 1

        set_examples true
        boot 2 || exit 1
        python "$HERE/seed.py" assert-shift "$WORLD" || exit 1
        require_verified 2 || exit 1

        boot 3 || exit 1
        python "$HERE/seed.py" assert-idempotent "$WORLD" || exit 1
        require_verified 3 || exit 1

        echo "postea migration gate: PASS"
        ;;
    world)
        set_world "${2:?world name}"
        grep '^level-name' "$SERVER/server.properties"
        ;;
    examples)
        set_examples "${2:?true or false}"
        grep registerExamples "$SERVER/config/materiallib.cfg"
        ;;
    boot)
        boot "${2:-manual}"
        ;;
    discover|assert-shift|assert-idempotent)
        python "$HERE/seed.py" "$1" "$WORLD"
        ;;
    seed)
        python "$HERE/seed.py" seed "$WORLD" "${2:-$WITNESS_ITEM}"
        ;;
    *)
        echo "unknown phase: $1" >&2
        exit 1
        ;;
esac
