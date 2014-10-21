/*
 * Copyright 2014 Cisco Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cisco.oss.foundation.orchestration.scope.utils

import com.google.common.collect.{ImmutableSet, Lists}
import com.google.common.util.concurrent.MoreExecutors._
import org.jclouds.ContextBuilder
import org.jclouds.concurrent.config.ExecutorServiceModule
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi
import org.jclouds.rackspace.clouddns.v1.domain.{CreateDomain, Record}
import org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates._
import org.jclouds.route53.Route53Api
import org.jclouds.route53.domain.ResourceRecordSet
import org.jclouds.sshj.config.SshjSshClientModule

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, future}

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 */
object DnsUtilsFactory {


  def instance() = {
    ScopeUtils.configuration.getString("cloud.provider") match {
      case "rackspace" => RackspaceDnsApi
      case "aws" => AwsDnsApi
      case "openstack" => NullDnsApi
      case "vsphere" => NullDnsApi
      case _ => throw new UnsupportedOperationException("Could NOT match provider DNS API. ( provider : " + ScopeUtils.configuration.getString("cloud.provider") + " )")
    }
  }
}

trait ScopeDnsApi extends Slf4jLogger {
  protected val modules = ImmutableSet.of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()), new SshjSshClientModule());
  protected val cloudProvider = ScopeUtils.configuration.getString("cloud.provider")
  protected val dnsProviderName = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.dns")

  protected val username: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.user")
  protected val password: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.password")

  protected val suffix = "vcs-foundation.com"
  protected val DNS_A_RECORD: String = "A"
  protected var context: ContextBuilder = null

  def init() {
    context = ContextBuilder.newBuilder(dnsProviderName)
      .credentials(username, password)
      .modules(modules)
  }

  def createDomain(systemId: String, instanceName: String, role: String)

  def deleteDns(dnsName: String)(implicit ec:ExecutionContext)

  def createARecord(systemId: String, instanceName: String, role: String, ip: String)(implicit ec:ExecutionContext)

  def createDnsName(role: String, instanceName: String, systemId: String): String = {
    role + "." + instanceName + "." + systemId + "." + suffix
  }
}

object RackspaceDnsApi extends ScopeDnsApi {
  init()
  private val dnsApi = context.buildApi(classOf[CloudDNSApi])


  def createDomain(systemId: String, instanceName: String, role: String) {
    val domainName = createDnsName(role, instanceName, systemId)

    val domains = dnsApi.getDomainApi().list().concat();
    domains.toList.foreach {
      case domain => {
        if (domain.getName().equalsIgnoreCase(domainName)) {
          return
        }
      }
    }

    val createDomain = CreateDomain.builder()
      .name(domainName)
      .email(s"$systemId@$suffix")
      .ttl(600000)
      .comment("Domain for " + systemId)
      //.records(createRecords)
      .build()

    val createDomains = ImmutableSet.of(createDomain)
    awaitComplete(dnsApi, dnsApi.getDomainApi().create(createDomains))
  }

  def deleteDns(dnsName: String)(implicit ec:ExecutionContext) {
    future {
      val domains = dnsApi.getDomainApi.listWithFilterByNamesMatching(dnsName).concat()
      val ids = Lists.newArrayList[Integer]()
      domains.toList.foreach {
        case domain => {
          ids.add(domain.getId)
        }
      }
      if (ids.size() > 0)
        dnsApi.getDomainApi.delete(ids, true)
    }
  }

  def createARecord(systemId: String, instanceName: String, role: String, ip: String)(implicit ec:ExecutionContext) {
    val domainName = createDnsName(role, instanceName, systemId)
    future {
      var domainId: Int = -1
      while (domainId == -1) {
        logger.trace("Search for Domain : {}", domainName)
        val domains = dnsApi.getDomainApi().list().concat();
        domains.foreach {
          case domain => {
            if (domain.getName().equalsIgnoreCase(domainName)) {
              domainId = domain.getId()
              logger.trace("Found Domain ID : {}", domainId.toString)
            }
          }
        }
        if (domainId == -1)
          Thread.sleep(1000)
      }


      val ARecord = Record.builder()
        .name(domainName)
        .`type`(DNS_A_RECORD)
        .data(ip)
        .ttl(300)
        .build()

      val records = ImmutableSet.of(ARecord)
      awaitComplete(dnsApi, dnsApi.getRecordApiForDomain(domainId).create(records))
    }
  }
}

object AwsDnsApi extends ScopeDnsApi {
  init()
  private val dnsApi = context.buildApi(classOf[Route53Api])


  def createDomain(systemId: String, instanceName: String, role: String) {
    val domainName = createDnsName(role, instanceName, systemId)
    val zones = dnsApi.getHostedZoneApi().list().concat()		
    zones.toList().foreach {
      case zone => {
        if (zone.getName().substring(0, zone.getName().length()-1).equalsIgnoreCase(domainName)) {
          logInfo(s"hosted zone $domainName already exists.  Not creating")
          return
        } else {
          logInfo(s"found non-matching dns name: $zone")          
        }
      }
    }
    logInfo(s"Creating hosted zone for $domainName")
    dnsApi.getHostedZoneApi().createWithReferenceAndComment(domainName, s"$domainName-$systemId@$suffix", s"domain for $systemId")
  }

  def deleteDns(dnsName: String)(implicit ec:ExecutionContext) {
    var zoneId: String = null
	logger.trace("Search for Domain : {}", dnsName)
	val zones = dnsApi.getHostedZoneApi().list().concat()
	zones.toList().foreach {
	  case zone => {
	    if (zone.getName().substring(0, zone.getName().length()-1).equalsIgnoreCase(dnsName)) {
	      zoneId = zone.getId()
	      logger.trace("Found Domain ID : {}", zoneId.toString)
	    }
	  }
	}
    if (zoneId == null)
      return
      
    val rrss = dnsApi.getResourceRecordSetApiForHostedZone(zoneId).list().concat()
    rrss.toList().foreach {
      case rrs => {
        if (DNS_A_RECORD.equals(rrs.getType())) {
        	dnsApi.getResourceRecordSetApiForHostedZone(zoneId).delete(rrs)
        }
      }
    }
   	dnsApi.getHostedZoneApi().delete(zoneId)
  }

  def createARecord(systemId: String, instanceName: String, role: String, ip: String)(implicit ec:ExecutionContext) {
	  val domainName = createDnsName(role, instanceName, systemId)

      var zoneId: String = null
      while (zoneId == null) {
        val zones = dnsApi.getHostedZoneApi().list().concat()
        zones.toList().foreach {
          case zone => {
            if (zone.getName().substring(0, zone.getName().length()-1).equalsIgnoreCase(domainName)) {
              zoneId = zone.getId()
            }
          }
        }
        if (zoneId == null)
          Thread.sleep(1000)
      }
      val rrs = ResourceRecordSet.builder()
    		      .name(s"${domainName}")
    		      .values(List(s"$ip") )
    		      .`type`(DNS_A_RECORD)
    		      .ttl(300)
    		      .build()
      dnsApi.getResourceRecordSetApiForHostedZone(zoneId).create(rrs)
         		      
        
  }
}

object NullDnsApi extends ScopeDnsApi {

  def createDomain(systemId: String, instanceName: String, role: String) {
    val domainName = createDnsName(role, instanceName, systemId)

    logWarn("Null implementation for create domain: {}", domainName)
  }

  def deleteDns(dnsName: String)(implicit ec:ExecutionContext) {
    logWarn("Null implementation for delete domain: {}", dnsName)
  }

  def createARecord(systemId: String, instanceName: String, role: String, ip: String)(implicit ec:ExecutionContext) {
    val domainName = createDnsName(role, instanceName, systemId)
    logWarn("Null implementation for create A record: {}", domainName)
  }
}