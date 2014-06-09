#!/bin/bash

# set default classpath if your CLASSPATH differs, modify script or run CLASSPATH=<class-path> ./average.sh

set -e

COMMAND="$(basename $0)"

HELP="[VARIABLES=<values>] $COMMAND [-h] {-p PATTERN | -f FILE_LIST} -s STEP [-j JOB_DIRECTORY] [-c COMMAND_FILE_DIR] [-b BATCH_SIZE] -- do some crazy averaging in z

where:
    -c COMMAND_FILE_DIR   specify directory for .cmd files generated in the process. defaults to $PWD
    -h                    show this help text
    -j JOB_DIRECTORY      directory for job specific files, defaults to /tmp/some/dir. Specify -j if you have jobs that run even after this script has finished (e.g. qsub).
    -f FILE_LIST          read filenames from file FILE_LIST (mutually exclusive with -p)
    -p PATTERN            read filenames from PATTERN (mutually exclusive with -f)
    -s STEP               step in x and y direction
    -n CORES              number of cores (default=$(( $(lscpu -p | tail -n1 | cut -f1 -d,) + 1)))
    -o OUTPUT_DIRECTORY   specify output directory
    -b BATCH_SIZE         instead of one java call per job, submit the calls in batches (Default: 1)

VARIABLES:
    CLASSPATH\t\t\tAdjust classpath to your system.
    CLASSPATH components (ignored, if CLASSPATH is specified):
    \tCONVENIENCE_JAR_PATH\t\tpath to convenience jar file
    \tIJ_JAR_PATH\t\t\tpath to imagej jar file
    \tIMGLIB_JAR_PATH\t\t\tpath to imglib2 jar file
    \tIMGLIB_IJ_JARPATH\t\tpath to imglib2-ij jar file

    EXEC_COMMAND\t\tcommand to run generated .cmd files (defaults to qsub)
    VERBOSE\t\t\tWill trigger alibi logging when non-empty

Specify CLASSPATH to adjust the script to your machine or CONVENIENCE_JAR_PATH, IJ_JAR_PATH, IMGLIB_JAR_PATH, IMGLIB_IJ_JARPATH from which CLASSPATH will be constructed. VERBOSE=<non-empty> will trigger alibi logging. EXEC_COMMAND will run the generated script (defaults to qsub).
"

unset VARIABLE_STEP
while getopts ":b:c:f:ho:p:n:s:v:j:" option; do
    case $option in
        b)
            BATCH_SIZE="$OPTARG"
            ;;
        c)
            COMMAND_FILE_DIR="$OPTARG"
            ;;
        h)
            echo -e "$HELP"
            exit 0
            ;;
        j)
            JOB_DIRECTORY="$OPTARG"
            ;;
        n)
            N_CORES="$OPTARG"
            ;;
        o)
            OUTPUT_DIRECTORY="$OPTARG"
            ;;
        p)
            PATTERN="$OPTARG"
            ;;
        f)
            FILE_LIST="$OPTARG"
            ;;
        s)
            STEP="$OPTARG"
            ;;
        v)
            VARIABLE_STEP_LIST="$OPTARG"
            ;;
    esac
done

CONVENIENCE_JAR_PATH="${CONVENIENCE_JAR_PATH:-/groups/saalfeld/home/hanslovskyp/.m2/repository/saalfeldlab/scaling/0.0.1-SNAPSHOT/scaling-0.0.1-SNAPSHOT.jar}"
IJ_JAR_PATH="${IJ_JAR_PATH:-/groups/saalfeld/home/hanslovskyp/.m2/repository/net/imagej/ij/1.48v/ij-1.48v.jar}"
IMGLIB_JAR_PATH="${IMGLIB_JAR_PATH:-/groups/saalfeld/home/hanslovskyp/.m2/repository/net/imglib2/imglib2/2.0.0-SNAPSHOT/imglib2-2.0.0-SNAPSHOT.jar}"
IMGLIB_IJ_JAR_PATH="${IMGLIB_IJ_JAR_PATH:-/groups/saalfeld/home/hanslovskyp/.m2/repository/net/imglib2/imglib2-ij/2.0.0-beta-27-SNAPSHOT/imglib2-ij-2.0.0-beta-27-SNAPSHOT.jar}"
# DEFAULT_CLASSPATH="/groups/saalfeld/home/hanslovskyp/workspace/convenience/target/convenience-0.0.1-SNAPSHOT.jar:/share/hanslovskyp/local/Fiji.app/jars/ij-1.48v.jar:/share/hanslovskyp/local/Fiji.app/jars/imglib2-2.0.0-beta-25.jar:/share/hanslovskyp/local/Fiji.app/jars/imglib2-ij-2.0.0-beta-25.jar"
DEFAULT_CLASSPATH="$CONVENIENCE_JAR_PATH:$IJ_JAR_PATH:$IMGLIB_JAR_PATH:$IMGLIB_IJ_JAR_PATH"
CLASSPATH="${CLASSPATH:-$DEFAULT_CLASSPATH}"
N_CORES="${N_CORES:-$(( $(lscpu -p | tail -n1 | cut -f1 -d,) + 1))}"
OUTPUT_DIRECTORY="${OUTPUT_DIRECTORY:-$PWD}"
COMMAND_FILE_DIR="${COMMAND_FILE_DIR:-$PWD}"
BATCH_SIZE="${BATCH_SIZE:-1}"


N_SLOTS="${N_CORES:-16}"
N_SLOTS="$(( $N_SLOTS > 16 ? 16 : $N_SLOTS ))"
DEFAULT_COMMAND="qsub -pe batch $N_SLOTS -b y -cwd -V -l short=true"
    # -pe batch: choose number of slots per node
    # -N name job
    # -b y: command can be script or binary
    # -cwd: execute job from current working directory
    # -V: export environment variables
EXEC_COMMAND="${EXEC_COMMAND:-$DEFAULT_COMMAND}"


if [ -n "$FILE_LIST" -a -n "$PATTERN" ]; then
    echo "Cannot specify -p (pattern) and -f (file list) at the same time!"
    echo
    echo -e "$HELP"
    exit 1
fi

if [ -z "$FILE_LIST" -a -z "$PATTERN" ]; then
    echo "Need to specify either -p (pattern) or -f (file list)!"
    echo
    echo -e "$HELP"
    exit 1
fi

if [ -z "$STEP" ]; then
    echo "Need to specify either -s (step)!"
    echo
    echo -e "$HELP"
    exit 1
fi

TMP_DIR="$(mktemp -d)"
trap "rm -rf $TMP_DIR" EXIT
JOB_DIRECTORY="${JOB_DIRECTORY:-$TMP_DIR}"
JOB_DIRECTORY="${JOB_DIRECTORY%/}"

mkdir -p "$JOB_DIRECTORY"
mkdir -p "$COMMAND_FILE_DIR"
mkdir -p "$OUTPUT_DIRECTORY"


if [ -n "$PATTERN" ]; then
    FILE_LIST="$JOB_DIRECTORY/file_list"
    ls $PATTERN | sort -n > $FILE_LIST
fi

N_FILES="$(cat $FILE_LIST | wc -l )"


COUNT=0
CMD_FILE_COUNT=0
JAVA_CMD=":" # no-op
while read line; do
    if [[ -n "$VERBOSE" ]]; then
        echo "$COUNT  --  $line"
    fi
    CURR_FILE_LIST="$(printf $JOB_DIRECTORY/%06d $COUNT)"
    echo "$line" > "$CURR_FILE_LIST"
    CURR_OUT_FILE="$(printf $OUTPUT_DIRECTORY/result_%06d.tif $COUNT)"
    JAVA_CMD="$JAVA_CMD &&\njava -cp \"$CLASSPATH\" \"org.janelia.scaling.ZScale\" \"$CURR_FILE_LIST\" \"$CURR_OUT_FILE\" \"$N_CORES\" \"binaryXY\" \"$STEP\""
    COUNT="$(( $COUNT + 1 ))"
    # reset JAVA_CMD if batch is full
    if [ "$(( $COUNT % $BATCH_SIZE ))" -eq "0" ]; then
        CURR_CMD_FILE="$(printf $COMMAND_FILE_DIR/command_%06d.cmd $CMD_FILE_COUNT)"
        echo -e "$JAVA_CMD" > "$CURR_CMD_FILE"
        chmod +x "$CURR_CMD_FILE"
        $EXEC_COMMAND $CURR_CMD_FILE
        JAVA_CMD=":"
        CMD_FILE_COUNT="$(( $CMD_FILE_COUNT + 1))"
        if [[ -n "$VERBOSE" ]]; then
            echo "    running   '$EXEC_COMMAND $CURR_CMD_FILE'"
        fi
    fi
done < $FILE_LIST

if  [ "$(( $COUNT % $BATCH_SIZE ))" -ne "0" ]; then
    CURR_CMD_FILE="$(printf $COMMAND_FILE_DIR/command_%06d.cmd $CMD_FILE_COUNT)"
    echo -e "$JAVA_CMD" > "$CURR_CMD_FILE"
    chmod +x "$CURR_CMD_FILE"
    $EXEC_COMMAND $CURR_CMD_FILE
    if [[ -n "$VERBOSE" ]]; then
        echo "    running   '$EXEC_COMMAND $CURR_CMD_FILE'"
    fi
fi

if [[ -n "$VERBOSE" ]]; then
    echo -e "TMP_DIR:\t\t$TMP_DIR"
    echo -e "JOB_DIRECTORY:\t\t$JOB_DIRECTORY"
    echo -e "CMD_DIRECTORY:\t\t$COMMAND_FILE_DIR"
fi

rm -rf $TMP_DIR

