#!/bin/bash
./script/astyle --mode=java -SpH -n -r "../*.java" -xi --exclude="build" --exclude="output"
