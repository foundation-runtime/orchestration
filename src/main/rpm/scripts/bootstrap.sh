yum -y install nds_blin_env
yum -y install nds_util_ndsps
yum -y install nds_mon_mama
cp /opt/nds/mama/docs/sample/* /opt/nds/mama/etc/.
service nds_mama start
/sbin/chkconfig nds_mama on