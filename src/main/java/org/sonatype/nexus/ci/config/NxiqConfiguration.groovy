/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.ci.config

import javax.annotation.Nullable

import com.sonatype.nexus.api.exception.IqClientException
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.IqUtil

import hudson.Extension
import hudson.model.Describable
import hudson.model.Descriptor
import hudson.util.FormValidation
import hudson.util.FormValidation.Kind
import hudson.util.ListBoxModel
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static GlobalNexusConfiguration.globalNexusConfiguration

class NxiqConfiguration
    implements Describable<NxiqConfiguration>
{
  String serverUrl

  boolean isPkiAuthentication

  String credentialsId

  @DataBoundConstructor
  NxiqConfiguration(final String serverUrl,
                    final boolean isPkiAuthentication,
                    final String credentialsId)
  {
    this.serverUrl = serverUrl
    this.isPkiAuthentication = isPkiAuthentication
    this.credentialsId = isPkiAuthentication ? null : credentialsId
  }

  @Override
  Descriptor<NxiqConfiguration> getDescriptor() {
    return jenkins.model.Jenkins.getInstance().getDescriptorOrDie(this.getClass())
  }

  static @Nullable URI getServerUrl() {
    def serverUrl = getIqConfig()?.@serverUrl
    serverUrl ? new URI(serverUrl) : null
  }

  /**
   * Always false. Enforced at the UI level until nexus-java-api supports PKI
   */
  static boolean getIsPkiAuthentication() {
    return getIqConfig()?.@isPkiAuthentication
  }

  static @Nullable String getCredentialsId() {
    getIqConfig()?.@credentialsId
  }

  static @Nullable NxiqConfiguration getIqConfig() {
    return globalNexusConfiguration.iqConfigs?.find { true }
  }

  @Extension
  static class DescriptorImpl
      extends Descriptor<NxiqConfiguration>
  {
    @Override
    String getDisplayName() {
      Messages.NxiqConfiguration_DisplayName()
    }

    @SuppressWarnings('unused')
    FormValidation doCheckServerUrl(@QueryParameter String value) {
      def validation = FormUtil.validateUrl(value)
      if (validation.kind == Kind.OK) {
        validation = FormUtil.validateNotEmpty(value, Messages.Configuration_ServerUrlRequired())
      }
      return validation
    }

    @SuppressWarnings('unused')
    ListBoxModel doFillCredentialsIdItems(@QueryParameter String serverUrl,
                                          @QueryParameter String credentialsId) {
      return FormUtil.newCredentialsItemsListBoxModel(serverUrl, credentialsId)
    }

    @SuppressWarnings('unused')
    FormValidation doVerifyCredentials(
        @QueryParameter String serverUrl,
        @QueryParameter @Nullable String credentialsId) throws IOException
    {
      try {
        def applications = IqUtil.getApplicableApplications(serverUrl, credentialsId)

        return FormValidation.ok(Messages.NxiqConfiguration_ConnectionSucceeded(applications.size()))
      }
      catch (IqClientException e) {
        return FormValidation.error(e, Messages.NxiqConfiguration_ConnectionFailed())
      }
    }
  }
}
