class admin::yum {

  $repo_url = hiera('bin_repo::url')
  $repo_name = hiera('bin_repo::name','base')
  file { "/etc/yum.repos.d/scope_product_$repo_name.repo":
    content => "[scope_product_$repo_name]
name=Scope Product Repo $repo_name
baseurl=${repo_url}
enabled=1
gpgcheck=0
",
  }


}
