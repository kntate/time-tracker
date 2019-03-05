BASE_URL="http://localhost:8080/api"

if [ "$1" == "print" ]; then
	curl ${BASE_URL}/print
	exit
fi

if [ "$1" == "yesterday" ]; then
	curl ${BASE_URL}/yesterday?outTime=${2}
	exit
fi

if [ "$1" == "testend" ]; then
	echo curl ${BASE_URL}/end?outTime=${2}
	curl ${BASE_URL}/testEnd?outTime=${2}
	exit
fi

if [ "$1" == "end" ]; then
	curl ${BASE_URL}/end?outTime=${2}
	exit
fi

if [ "$1" == "start" ]; then
	curl ${BASE_URL}/start?inTime=${2}
	exit
fi

if [ "$1" == "holiday" ]; then
	curl ${BASE_URL}/holiday?date=${2}
	exit
fi

if [ "$1" == "pto" ]; then
	curl ${BASE_URL}/pto?date=${2}
	exit
fi

mvn clean install -f /c/workspaces/g2/time-tracker/pom.xml ;java -jar /c/workspaces/g2/time-tracker/target/time-tracker-0.0.1-SNAPSHOT.jar
