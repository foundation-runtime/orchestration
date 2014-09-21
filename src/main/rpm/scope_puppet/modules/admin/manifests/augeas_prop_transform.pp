define admin::augeas_prop_transform($source, $changes, $destination = $title) {

    exec { "/usr/bin/dos2unix -n ${source} ${destination}":
        user => "ndsuser",
        logoutput => true,
        onlyif => "/usr/bin/test ${source} -nt ${destination}",
    } ->

    augeas { "${destination}":
        incl    => "${destination}",
        lens    => "Properties.lns",
        changes => $changes,
    }

}

