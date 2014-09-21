class admin::ssh_disable_host_key_check() {

  $host_pattern = hiera('host_pattern')
  $host_pattern_quot = "'$host_pattern'"

  augeas { 'ssh_disable_host_key_check':
    context => '/files/etc/ssh/ssh_config',
    changes => [
      "set Host[. = $host_pattern_quot] $host_pattern_quot",
      "set Host[. = $host_pattern_quot]/UserKnownHostsFile /dev/null",
      "set Host[. = $host_pattern_quot]/StrictHostKeyChecking no",
    ],
  }

}
