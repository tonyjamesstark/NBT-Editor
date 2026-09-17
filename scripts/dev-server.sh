#!/usr/bin/env bash
# Start the dev dedicated server and block until it is accepting connections.
#
# Exists so the client harness has a world to join, which is where item components
# are bound and where the join-time half of mod startup runs. Exits 0 once the
# server logs "Done", 1 on a crash or a timeout. Leave it running; Ctrl-C stops it.
#
# Usage: scripts/dev-server.sh [timeout-seconds]
set -uo pipefail
cd "$(dirname "$0")/.."

DEADLINE=${1:-300}
LOG=run/dev-server.log

export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
export PATH="$JAVA_HOME/bin:$PATH"

mkdir -p run/server
echo eula=true > run/server/eula.txt

# A one-layer flat world so spawn preparation is a second rather than a minute.
# generator-settings has to be spelled out; without it the flat preset parses as
# empty and the server logs "No key layers in MapLike[{}]".
cat > run/server/server.properties <<'PROPS'
online-mode=false
level-type=minecraft\:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
spawn-protection=0
max-players=2
view-distance=4
simulation-distance=4
sync-chunk-writes=false
PROPS

rm -f "$LOG"
./gradlew runServer --console=plain >"$LOG" 2>&1 &
gradle=$!

started=$SECONDS
while (( SECONDS - started < DEADLINE )); do
	if grep -q 'Done (' "$LOG" 2>/dev/null; then
		echo "server up in $((SECONDS - started))s on 127.0.0.1:25565"
		wait "$gradle"
		exit $?
	fi
	if ! kill -0 "$gradle" 2>/dev/null; then
		echo "the server exited after $((SECONDS - started))s"
		tail -25 "$LOG"
		exit 1
	fi
	sleep 3
done

echo "timed out after ${DEADLINE}s short of accepting connections"
tail -25 "$LOG"
exit 1
