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

import org.slf4j.{ Logger, LoggerFactory }
import org.slf4j.Marker

/**
 * Use this trait in any class to use the logging APi directly as if it was defined in your own class. 
 * Usage Example:<br>
 * <code>
 * 		logTrace("trace test")
		logTrace("trace test: {}", "1")
		logTrace("trace test {} and {}", "1","2")
		logTrace("trace test {} and {} and {}", "1","2","3")
		
		logDebug("debug test")
		logDebug("debug test: {}", "1")
		logDebug("debug test {} and {}", "1","2")
		logDebug("debug test {} and {} and {}", "1","2","3")
		
		logInfo("info test")
		logInfo("info test: {}", "1")
		logInfo("info test {} and {}", "1","2")
		logInfo("info test {} and {} and {}", "1","2","3")
		
		logWarn("warn test")
		logWarn("warn test: {}", "1")
		logWarn("warn test {} and {}", "1","2")
		logWarn("warn test {} and {} and {}", "1","2","3")
		
		logError("error test")
		logError("error test: {}", "1")
		logError("error test {} and {}", "1","2")
		logError("error test {} and {} and {}", "1","2","3")
 * </code>>
 */
trait Slf4jLogger {

	@transient final lazy val logger: Logger = LoggerFactory.getLogger(getClass)

	def logTrace(message: => String, args: AnyRef*) = {
		if (logger.isTraceEnabled) {
			logger.trace(message, args:_*)
		}
	}

	def logTrace(message: => String, throwable: Throwable) = {
		if (logger.isTraceEnabled) {
			logger.trace(message, throwable)
		}
	}
	
	def logTrace(marker: Marker, message: => String, throwable: Throwable) = {
		if (logger.isTraceEnabled) {
			logger.trace(marker, message, throwable)
		}
	}

	def logDebug(message: => String, args: AnyRef*) = {
		if (logger.isDebugEnabled) {
			logger.debug(message, args:_*)
		}
	}

	def logDebug(message: => String, throwable: => Throwable) = {
		if (logger.isDebugEnabled) {
			logger.debug(message, throwable)
		}
	}
	
	def logDebug(marker: Marker, message: => String, throwable: Throwable) = {
		if (logger.isDebugEnabled) {
			logger.debug(marker, message, throwable)
		}
	}

	def logInfo(message: => String, args: AnyRef*) = {
		if (logger.isInfoEnabled) {
			logger.info(message, args:_*)
		}
	}

	def logInfo(message: => String, throwable: => Throwable) = {
		if (logger.isInfoEnabled) {
			logger.info(message, throwable)
		}
	}
	
	def logInfo(marker: Marker, message: => String, throwable: Throwable) = {
		if (logger.isInfoEnabled) {
			logger.info(marker, message, throwable)
		}
	}

	def logWarn(message: => String, args: AnyRef*) = {
		if (logger.isWarnEnabled) {
			logger.warn(message, args:_*)
		}
	}

	def logWarn(message: => String, throwable: => Throwable) = {
		if (logger.isWarnEnabled()) {
			logger.warn(message, throwable)
		}
	}
	
	def logWarn(marker: Marker, message: => String, throwable: Throwable) = {
		if (logger.isWarnEnabled) {
			logger.warn(marker, message, throwable)
		}
	}

	def logError(message: => String, args: AnyRef*) = {
		if (logger.isErrorEnabled) {
			logger.error(message, args:_*)
		}
	}

	def logError(message: => String, throwable: Throwable) = {
		if (logger.isErrorEnabled()) {
			logger.error(message, throwable)
		}
	}
	
	def logError(marker: Marker, message: => String, throwable: Throwable) = {
		if (logger.isErrorEnabled) {
			logger.error(marker, message, throwable)
		}
	}

}
