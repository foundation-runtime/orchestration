
basedir=`pwd`
mkdir -p repos/yum/components
mkdir -p repos/prodpuppet/modules

cd modules

for m in *
do
   ln -sf -T `pwd`/$m/bin $basedir/repos/yum/components/$m
   cp -R `pwd`/$m/puppet $basedir/repos/prodpuppet/modules/$m
done

git config --global user.email "scope@vcs-foundation.com"
git config --global user.name "scope"

cd $basedir/repos/prodpuppet
git init
git add *
git commit -m "Init prodpuppet repo"
git update-server-info

cd /etc/puppet/chpuppet
git update-server-info

ln -sf -T $basedir/yum $basedir/repos/yum/dependencies

yum -y install createrepo

cd $basedir/repos/yum/components
createrepo .

cd $basedir/repos/yum/dependencies
createrepo .

yum -y install nginx

echo "
user              nginx;
worker_processes  1;

error_log  /var/log/nginx/error.log;

pid        /var/run/nginx.pid;

events {
    worker_connections  1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    access_log  /var/log/nginx/access.log;

    sendfile        on;
    keepalive_timeout  65;

    server {
        listen       80;
        server_name  _;

        location / {
            root   /usr/share/nginx/html;
            index  index.html index.htm;
        }

        location /components {
            autoindex on;
            alias $basedir/repos/yum/components;
        }

        location /dependencies {
            autoindex on;
            alias $basedir/repos/yum/dependencies;
        }

        location /chpuppet {
            autoindex on;
            alias /etc/puppet/chpuppet/.git;
        }

        location /prodpuppet {
            autoindex on;
            alias $basedir/repos/prodpuppet/.git;
        }
        
        error_page  404              /404.html;
        location = /404.html {
            root   /usr/share/nginx/html;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   /usr/share/nginx/html;
        }
    }
}

" > /etc/nginx/nginx.conf

service nginx restart

exit
