define admin::augeas_prop_transform($source, $changes, $destination = $title, $user = "ndsuser", $group = "nds" ) {

  exec { "/usr/bin/dos2unix -n ${source} ${destination}":
    user => "${user}",
    group => "${group}",
    logoutput => true,
    onlyif => "/usr/bin/test ${source} -nt ${destination}",
  } ->

  augeas { "${destination}":
    incl    => "${destination}",
    lens    => "Properties.lns",
    changes => $changes,
  }

}

