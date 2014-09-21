class admin::ntp {

    $ntp_server1 = hiera("ntp::server1")

    augeas { "ntp.servers":
        context  => "/files/etc/ntp.conf",
        changes => [
            "set server[1] $ntp_server1",
        ],
        notify => Service['ntpd'],
    }

    service { 'ntpd':
        ensure => running,
        enable => true,
    }

}

