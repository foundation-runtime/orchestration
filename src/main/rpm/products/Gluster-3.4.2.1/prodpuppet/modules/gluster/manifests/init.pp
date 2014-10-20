class gluster {
  package { 'glusterfs-server':
    provider => 'yum',
    ensure   => installed
  }
  service { 'glusterd':
    enable    => true,
    ensure    => running,
    hasstatus => true,
    require   => Package['glusterfs-server'],
  }
}