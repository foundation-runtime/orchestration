class gluster_configure {
  $peers_hiera = hiera('glusterfs::peers')
  if is_string( $peers_hiera) {
    $peers =  split( $peers_hiera, ';;')
  } else{
    $peers = $peers_hiera
  }

  $peers_size = size($peers)

  $bricksDirectory = hiera('glusterfs::bricksDirectory', '/data/brick1')
  $user = hiera('glusterfs::bricksDirectory::user', 'ndsuser')
  $group = hiera('glusterfs::bricksDirectory::group', 'nds')

  $volumeName = hiera('glusterfs::volume_name', 'vg0')

  $mountDirectory = hiera('glusterfs::mount::directory', '/data/derby')

  gluster_configure::add_probe{ "add_probes":
    endpoints => $peers,
    before    => File["${bricksDirectory}"]
  }

  exec { "exec_${bricksDirectory}":
    command => "mkdir -p ${bricksDirectory}",
    creates => "${bricksDirectory}",
    path    => [ '/usr/sbin', '/usr/bin', '/sbin', '/bin' ],
    before  => File["${bricksDirectory}"]
  }

  file { "${bricksDirectory}":
    owner   => $user,
    group   => $group,
    ensure  => directory,
    before  => File["${bricksDirectory}/${volumeName}"]
  }

  file { "${mountDirectory}":
    owner   => $user,
    group   => $group,
    ensure  => directory,
    before  => Exec["mount_${mountDirectory}"]
  }

  file { "${bricksDirectory}/${volumeName}":
    owner   => $user,
    group   => $group,
    ensure  => directory,
    before  => Exec["gluster_volume_create_${volumeName}"]
  }

  augeas { "selinux_config":
    context => "/files/etc/selinux/config",
    changes => [
      "set SELINUX permissive",
    ],
  }

  $new_peers = suffix($peers,":${bricksDirectory}/${volumeName}")
  $servers_list = inline_template('<%= new_peers.join(" ") %>')

  exec { "gluster_volume_create_${volumeName}":
  # gluster volume create gv0 replica 2 server1:/data/brick1/gv0 server2:/data/brick1/gv0
    command => "/usr/sbin/gluster volume create ${volumeName} replica ${peers_size} ${servers_list} force",
    creates => "/var/lib/glusterd/vols/${volumeName}",
    before  => Exec["gluster_volume_start_${volumeName}"]
  }

  exec { "gluster_volume_start_${volumeName}":
    command =>  "/usr/sbin/gluster volume start ${volumeName}",
    unless  => "[ \"`gluster volume info ${volumeName} | egrep '^Status:'`\" == 'Status: Started' ]",
    path    => [ '/usr/sbin', '/usr/bin', '/sbin', '/bin' ],
    before  => Exec["mount_${mountDirectory}"]
  }

  exec { "mount_${mountDirectory}":
    command =>  "mount -t glusterfs ${::hostname}:${volumeName} ${mountDirectory}",
    unless  => "mount | grep ${volumeName}",
    path    => [ '/usr/sbin', '/usr/bin', '/sbin', '/bin' ],
  }


}