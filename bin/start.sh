#!/bin/bash

# 使用JAVA_HOME来获取java可执行文件的路径
if [ -z "$JAVA_HOME" ]; then
  javaExe=java
else
  javaExe=$JAVA_HOME/bin/java
fi

# 应用根目录
appPath=$(dirname "$(pwd)")
# 应用启动类
appStarter="com.siaor.poetize.next.PoetizeNextApplication"
# JVM参数（例如：内存大小）
jvmOpts="-Xms512m -Xmx1024m"

# 进入项目根目录
cd "$appPath" || exit

# 启动应用
nohup $javaExe -server $jvmOpts -Dfile.encoding=UTF-8 -cp "$appPath/lib/*" "$appStarter" &

# 获取应用的进程ID以便后续可能的操作
PID=$!
echo "PoetizeNext启动成功！PID: $PID"
