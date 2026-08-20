@echo off
echo.
echo [��Ϣ] ʹ��Jar��������Web���̡�
echo.

cd %~dp0
cd ../ruoyi-admin/target

rem -Duser.timezone=Asia/Shanghai: 固定 JVM 时区为 GMT+8（宿主 OS 时区可能为 UTC，会导致 JDBC 写入与当日边界计算偏移 8 小时）
set JAVA_OPTS=-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -Duser.timezone=Asia/Shanghai

java -jar %JAVA_OPTS% ruoyi-admin.jar

cd bin
pause