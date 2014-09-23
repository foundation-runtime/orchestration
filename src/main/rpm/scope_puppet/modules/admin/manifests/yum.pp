class admin::yum {

  $repo_url = hiera('bin_repo::url')
  file { "/etc/yum.repos.d/scope_product.repo":
    content => "[scope_product]
name=Scope Product Repo
baseurl=${repo_url}
enabled=1
gpgcheck=0
",
  }


}
