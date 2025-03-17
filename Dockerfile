# 已经安装好Nginx、JDK的基础镜像
FROM deb-web:12.9.1

# 维护者信息
LABEL maintainer="siaor@qq.com"

# 配置Nginx
COPY ./dev/nginx/nginx.conf /etc/nginx/nginx.conf

# 复制安装包目录并自动解压
ADD ./target/poetize-next-2.0.250313.1.tar.gz /app

# 暴露端口
EXPOSE 80 443

# 工作目录
WORKDIR /app/poetize-next

# 启动
ENTRYPOINT ["/bin/sh","-c","/usr/sbin/nginx;java -server -Dfile.encoding=UTF-8 -cp 'lib/*' com.siaor.poetize.next.PoetizeNextApplication"]

# 构建命令：docker build -t poetize-next:2.0.250315.1 .
# 运行目录：docker run -d -it -p 80:80 -p 443:443 --restart=always --name=pn -v /app/poetize-next:/app/poetize-next poetize-next:2.0.250315.1