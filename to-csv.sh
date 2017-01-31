#!/bin/bash

FILENAME=$1

paste -d, <(grep 'Collect' "$FILENAME" | sed 's/^.*: //') <(grep 'Mediate' "$FILENAME" | sed 's/^.*: //')
