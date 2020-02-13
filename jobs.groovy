def enabled = false;
def githubProjects = !enabled ? new groovy.json.JsonSlurper().parseText("{}") : new groovy.json.JsonSlurper().parse(
  (new URL("https://raw.githubusercontent.com/halkeye/jenkins-jobs/master/github_projects.json")).newReader()
);
def bitbucketProjects = !enabled ? new groovy.json.JsonSlurper().parseText("{}") : new groovy.json.JsonSlurper().parse(
  (new URL("https://raw.githubusercontent.com/halkeye/jenkins-jobs/master/bitbucket_projects.json")).newReader()
);
def githubOrgs = !enabled ? new groovy.json.JsonSlurper().parseText("[]") : new groovy.json.JsonSlurper().parse(
  (new URL("https://raw.githubusercontent.com/halkeye/jenkins-jobs/master/github_orgs.json")).newReader()
);

folder("github_projects") {
  authorization {
    ["nfg", "aliaoca", "authorized", "authenticated"].each { user ->
      permission("hudson.model.Item.Read", user)
      permission("hudson.model.Item.Discover", user)
    }
  }
  displayName("Github Projects")
  description("All the github projects")
}

folder("github_orgs") {
  authorization {
    ["nfg", "aliaoca", "authorized", "authenticated"].each { user ->
      permission("hudson.model.Item.Read", user)
      permission("hudson.model.Item.Discover", user)
    }
  }
  displayName("Github Organization Projects")
  description("")
}

folder("bitbucket_projects") {
  displayName("Bitbucket Projects")
  description("All the bitbucket projects")
}

githubProjects.keySet().each { username ->
  githubProjects[username].each { slug ->
    multibranchPipelineJob("github_projects/" + username + "_" + slug) {
      branchSources {
        branchSource {
          source {
            github {
              credentialsId("github-halkeye")
              configuredByUrl true
              repositoryUrl "https://github.com/" + username + "/" + slug
              repoOwner(username)
              repository(slug)
              traits {
                pruneStaleBranchTrait()
                gitHubBranchDiscovery {
                  strategyId(3)
                }
                gitHubTagDiscovery()
              }
            }
            buildStrategies {
              buildRegularBranches()
              buildTags {
                atLeastDays '-1'
                atMostDays '3'
              }
            }
          }
        }
      }
      configure {
        // workaround for JENKINS-46202 (https://issues.jenkins-ci.org/browse/JENKINS-46202)
        def traits = it / sources / data / 'jenkins.branch.BranchSource' / source / traits
        traits << 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait' {
          strategyId 2
          trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustPermission')
        }
        traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' {
          strategyId 4
        }
        traits << 'org.jenkinsci.plugins.githubScmTraitNotificationContext.NotificationContextTrait' {
          typeSuffix true
          contextLabel "continuous-integration/halkeye"
        }
        def buildStrategies = it / buildStrategies
          buildStrategies << 'jenkins.branch.NoTriggerBranchProperty' {
        }
      }
      orphanedItemStrategy {
        discardOldItems {
          numToKeep(5)
        }
      }
    }
  }
}

bitbucketProjects.keySet().each { username ->
  bitbucketProjects[username].each { slug ->
    multibranchPipelineJob("bitbucket_projects/" + username + "_" + slug) {
      branchSources {
        branchSource {
          source {
            bitbucket {
              credentialsId('bitbucket-halkeye')
              repoOwner(username)
              repository(slug)
              traits {
                pruneStaleBranchTrait()
              }
            }
            buildStrategies {
              buildRegularBranches()
              buildTags {
                atLeastDays '-1'
                atMostDays '3'
              }
            }
          }
        }
      }
      configure {
        def traits = it / sources / data / 'jenkins.branch.BranchSource' / source / traits
        traits << 'com.cloudbees.jenkins.plugins.bitbucket.BranchDiscoveryTrait' {
          strategyId(1) // detect all branches -refer the plugin source code for various options
        }
        traits << 'com.cloudbees.jenkins.plugins.bitbucket.ForkPullRequestDiscoveryTrait' {
          strategyId(1)
        }
        traits << 'com.cloudbees.jenkins.plugins.bitbucket.OriginPullRequestDiscoveryTrait' {
          strategyId(1)
        }
        traits << 'com.cloudbees.jenkins.plugins.bitbucket.WebhookRegistrationTrait' {
          mode(ITEM)
        }
        traits << 'jenkins.branch.NoTriggerBranchProperty' {
        }
        def buildStrategies = it / buildStrategies
        buildStrategies << 'jenkins.branch.NoTriggerBranchProperty' {
        }
      }
      orphanedItemStrategy {
        discardOldItems {
          numToKeep(5)
        }
      }
    }
  }
}

githubOrgs.each { slug ->
  organizationFolder("github_orgs/" + slug) {
    orphanedItemStrategy {
      defaultOrphanedItemStrategy {
        pruneDeadBranches(true)
        numToKeepStr("5")
        daysToKeepStr("")
      }
      discardOldItems {
        numToKeep(5)
      }
    }
    organizations {
      github {
        credentialsId("github-halkeye")
        repoOwner(slug)
        traits {
          pruneStaleBranchTrait()
          gitHubBranchDiscovery {
            strategyId(3)
          }
          gitHubTagDiscovery()
        }
      }
    }
    configure {
      def traits = it / navigators / 'org.jenkinsci.plugins.github__branch__source.GitHubSCMNavigator' / traits
      traits << 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait' {
          strategyId 3
      }
      traits << 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait' {
          strategyId 2
          trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustPermission')
      }
      traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' {
          strategyId 4
      }
      traits << 'org.jenkinsci.plugins.github__branch__source.TagDiscoveryTrait' {

      }

      traits << 'org.jenkinsci.plugins.githubScmTraitNotificationContext.NotificationContextTrait' {
        typeSuffix true
        contextLabel "continuous-integration/halkeye"
      }
      def buildStrategies = it / buildStrategies
      buildStrategies << 'jenkins.branch.buildstrategies.basic.BranchBuildStrategyImpl' {
      }
      buildStrategies << 'jenkins.branch.buildstrategies.basic.TagBuildStrategyImpl' {
        atLeastMillis 1
        atMostMillis 259200000
      }
      buildStrategies << 'jenkins.branch.NoTriggerBranchProperty' {
      }
    }
  }
}
