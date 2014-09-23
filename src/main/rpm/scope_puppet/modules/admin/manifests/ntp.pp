class admin::ntp {

  $ntp_server1 = hiera("ntp::server1", '')

  if ( $ntp_server1 == '') {
   debug("NTP configured by other.")
  } else {
    augeas { "ntp.servers":
      context  => "/files/etc/ntp.conf",
      changes  => [
        "set server[1] $ntp_server1",
      ],
      notify   => Service['ntpd'],
    }

    service { 'ntpd':
      ensure => running,
      enable => true,
    }
  }
}

