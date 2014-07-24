#!/bin/bash

# Find appropriate line number (== file number) for z slice. Counting starts at 0.

if [ "$#" -ne "2" ]; then
    exit 1
fi

STEP_FILE="$1"
Z_SLICE="$2"

LINE_COUNT=0
while read CURR_LINE; do
    CURR_MIN=$( echo $CURR_LINE | cut -d ',' -f1 )
    CURR_MAX=$( echo $CURR_LINE | cut -d ',' -f2 )
    if [ "$Z_SLICE" -ge "$CURR_MIN" -a "$Z_SLICE" -le "$CURR_MAX" ]; then
        break;
    fi
    LINE_COUNT="$(( LINE_COUNT + 1 ))"
done < $STEP_FILE

echo $LINE_COUNT
