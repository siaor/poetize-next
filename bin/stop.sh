#!/bin/bash

# 应用根目录
appPath=$(dirname "$(pwd)")

# 进入项目根目录
cd "$appPath" || exit

appPid=$(cat app.pid)
kill -9 $appPid  && echo "PoetizeNext已关闭...."

rm -f app.pid