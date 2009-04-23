#!/bin/sh -e
### BEGIN INIT INFO
# Provides:          webobjects
# Required-Start:    apache
# Required-Stop:     apache
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start/stop WebObjects
### END INIT INFO

# Source init function library.
. /lib/lsb/init-functions

export NEXT_ROOT="/opt"
#export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

USER=appserver

# See how we were called.
case "$1" in
	start)
		log_daemon_msg "Starting wotaskd and Monitor: "
		su $USER -c "$NEXT_ROOT/Library/WebObjects/JavaApplications/wotaskd.woa/wotaskd -WOPort 1085 &" &> /dev/null
		su $USER -c "$NEXT_ROOT/Library/WebObjects/JavaApplications/JavaMonitor.woa/JavaMonitor -WOPort 56789 &" &> /dev/null
		log_end_msg 0
		;;
	stop)
		log_daemon_msg "Shutting down wotaskd and Monitor: "
		WOTASKD_PID=`ps aux | awk '/WOPort 1085/ && !/awk/ {print $2}'`
		kill $WOTASKD_PID
		MONITOR_PID=`ps aux | awk '/WOPort 56789/ && !/awk/ {print $2}'`
		kill $MONITOR_PID
		log_end_msg 0
		;;
	restart)
		$0 stop
		$0 start
		;;
	*)
		echo -n "Usage: $0 {start|stop|restart}"
		exit 1
esac

if [ $# -gt 1 ]; then
	shift
	$0 $*
fi

exit 0
