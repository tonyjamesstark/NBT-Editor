#!/usr/bin/env bash
# Boot the dev client on a virtual framebuffer and report whether mod startup survived.
#
# The build host has no display, so Xvfb and Mesa's software rasteriser stand in for a
# GPU. That is enough to reach the title screen, which is all the startup path needs.
# Exits 0 when the client gets there, 1 on a crash or a timeout, with the decisive log
# lines either way.
#
# With --join it goes further and connects to a server, which is where item components
# get bound and the join-time half of startup runs. Success is the server logging the
# player in. A local dev server is started if one is not already up, and stopped again on
# the way out; pass a host:port to use one that is already running elsewhere.
#
# Usage: scripts/dev-client.sh [--join [host:port]] [timeout-seconds]
set -uo pipefail
cd "$(dirname "$0")/.."

JOIN=
if [ "${1:-}" = "--join" ]; then
	shift
	case "${1:-}" in
		*:*) JOIN=$1; shift ;;
		*) JOIN=127.0.0.1:25565 ;;
	esac
fi

DEADLINE=${1:-300}
LOG=run/dev-client.log
SERVER_LOG=run/dev-server.log
CRASHED='Minecraft Crash Report|Unreported exception thrown|Mixin apply for mod .* failed'

export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
export PATH="$JAVA_HOME/bin:$PATH"
export LIBGL_ALWAYS_SOFTWARE=1
export GALLIUM_DRIVER=llvmpipe

for n in $(seq 90 99); do
	[ -e "/tmp/.X11-unix/X$n" ] || { display=$n; break; }
done
[ -n "${display:-}" ] || { echo "no free X display between :90 and :99"; exit 1; }

Xvfb ":$display" -screen 0 1280x720x24 -nolisten tcp &
xvfb=$!
export DISPLAY=":$display"

server_is_ours=
cleanup() {
	pkill -f 'nbte\.devrun=client' 2>/dev/null
	[ -n "$server_is_ours" ] && pkill -f 'nbte\.devrun=server' 2>/dev/null
	kill "$xvfb" 2>/dev/null
}
trap cleanup EXIT

mkdir -p run
rm -f "$LOG"

# Two options the harness cannot afford to leave at their defaults. The accessibility
# onboarding screen is queued ahead of the quick-play join and would swallow it, and an
# Xvfb window never takes focus, so pausing on focus loss would freeze the client.
for opt in onboardAccessibility:false pauseOnLostFocus:false; do
	touch run/options.txt
	if grep -q "^${opt%%:*}:" run/options.txt; then
		sed -i "s|^${opt%%:*}:.*|$opt|" run/options.txt
	else
		echo "$opt" >> run/options.txt
	fi
done
if [ -n "$JOIN" ]; then
	if ! pgrep -f 'nbte\.devrun=server' >/dev/null; then
		echo "starting a dev server"
		server_is_ours=1
		# The old log has to go first. Both the readiness check below and the join
		# baseline below that read this file, and a previous run's "Done (" and
		# "joined the game" lines would satisfy each of them against a server that
		# is not up yet.
		rm -f "$SERVER_LOG"
		scripts/dev-server.sh "$DEADLINE" >| run/dev-server-harness.log 2>&1 &
		waited=$SECONDS
		until grep -q 'Done (' "$SERVER_LOG" 2>/dev/null; do
			(( SECONDS - waited < DEADLINE )) || { echo "the dev server never came up"; tail -25 "$SERVER_LOG"; exit 1; }
			sleep 3
		done
		echo "dev server up in $((SECONDS - waited))s"
	fi
	joined=$(grep -c 'joined the game' "$SERVER_LOG")
	./gradlew runClient --console=plain "-Pjoin=$JOIN" >"$LOG" 2>&1 &
else
	./gradlew runClient --console=plain >"$LOG" 2>&1 &
fi
gradle=$!

started=$SECONDS
while (( SECONDS - started < DEADLINE )); do
	if [ -f "$LOG" ] && grep -qE -- "$CRASHED" "$LOG"; then
		echo "FAILED after $((SECONDS - started))s"
		grep -nE -m1 -A22 -- "$CRASHED" "$LOG"
		exit 1
	fi
	if [ -n "$JOIN" ]; then
		if (( $(grep -c 'joined the game' "$SERVER_LOG") > joined )); then
			echo "OK, joined $JOIN in $((SECONDS - started))s"
			exit 0
		fi
	elif [ -f "$LOG" ] && grep -q 'Sound engine started' "$LOG"; then
		echo "OK, reached the title screen in $((SECONDS - started))s"
		exit 0
	fi
	if ! kill -0 "$gradle" 2>/dev/null; then
		echo "the client exited after $((SECONDS - started))s without a crash report"
		tail -25 "$LOG"
		exit 1
	fi
	sleep 3
done

echo "timed out after ${DEADLINE}s short of ${JOIN:+joining $JOIN}${JOIN:-the title screen}"
tail -25 "$LOG"
exit 1
