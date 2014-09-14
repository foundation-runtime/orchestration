define gluster_configure::add_probe(
$endpoints
) {
$count = size( $endpoints )

if "${count}" == "0" {
notify { "done!":
  message => "done!",
}
} else {
# recurse
$endpoint = values_at($endpoints, 0)
notify { "add_probe_${count}":
  message => "add endpoint ${endpoint}",
}
exec { "exec_${endpoint}":
  command => "/usr/sbin/gluster peer probe ${endpoint}",
  unless => "/bin/egrep '^hostname.+=${endpoint}$' /var/lib/glusterd/peers/*",
  tries => 20,
  try_sleep => 5,
}
$new_endpoints = delete_at($endpoints, 0)
gluster_configure::add_probe { "add_probe_${count}":
  endpoints=> $new_endpoints,
}
}
}
