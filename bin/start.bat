chcp 65001
@echo off

:: 使用JAVA_HOME来获取java可执行文件的路径
if "%JAVA_HOME%"=="" (
    set javaExe=java
) else (
    set javaExe=%JAVA_HOME%\bin\java
)

:: 应用启动类
set appStarter=com.siaor.poetize.next.PoetizeNextApplication

:: JVM参数（例如：内存大小）
set jvmOpts=-Xms512m -Xmx1024m

:: 进入项目根目录
cd /d %~dp0
cd ..

:: 启动应用（Windows不支持nohup，使用start /b来后台运行）
start /b %javaExe% -server %jvmOpts% -Dfile.encoding=UTF-8 -cp ".\lib\*" %appStarter%

echo "PoetizeNext加载中，请稍候。。。"

pause