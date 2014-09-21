
class admin::augeas {

    case $puppetversion {
        /Enterprise/: {
            $lensdir = '/opt/puppet/share/augeas/lenses/dist'
        }
        default: {
            $lensdir = '/usr/share/augeas/lenses/dist/'
        }
    }
   
    # deploy Properties lens file from augeas version 1.0.0
    # built-in Properties lens cannot parse UMs properties files
    # remove this and references once migrated to augeas 1.0.0 or later
    file { "${lensdir}/properties100.aug":
        owner 	=> "root",
        mode    => 0644,
        source => "puppet:///modules/admin/properties100.aug",
    }

}

