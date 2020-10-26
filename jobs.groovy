def enabled = true;
def githubProjects = !enabled ? new groovy.json.JsonSlurper().parseText("{}") : new groovy.json.JsonSlurper().parse(
  (new URL("https://raw.githubusercontent.com/halkeye/jenkins-jobs/master/github_projects.json")).newReader()
);
def bitbucketProjects = !enabled ? new groovy.json.JsonSlurper().parseText("{}") : new groovy.json.JsonSlurper().parse(
  (new URL("https://raw.githubusercontent.com/halkeye/jenkins-jobs/master/bitbucket_projects.json")).newReader()
);
def githubOrgs = !enabled ? new groovy.json.JsonSlurper().parseText("{}") : new groovy.json.JsonSlurper().parse(
  (new URL("https://raw.githubusercontent.com/halkeye/jenkins-jobs/master/github_orgs.json")).newReader()
);

def appEnabledAccounts = ["halkeye", "halkeye-helm-charts", "halkeye-docker"]

folder("github_projects") {
  authorization {
    ["nfg", "aliaoca", "anonymous", "authorized", "authenticated"].each { user ->
      permission("hudson.model.Item.Read", user)
      permission("hudson.model.Item.Discover", user)
    }
  }
  displayName("Github Projects")
  description("All the github projects")
}

folder("bitbucket_projects") {
  displayName("Bitbucket Projects")
  description("All the bitbucket projects")
}

githubProjects.keySet().each { username ->
  githubProjects[username].each { slug ->
    multibranchPipelineJob("github_projects/" + username + "_" + slug) {
      triggers {
        periodicFolderTrigger {
          interval("86400000")
        }
      }
      branchSources {
        branchSource {
          source {
            github {
              credentialsId(appEnabledAccounts.contains(username) ? "github-app-" + username : "github-halkeye")
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
              buildAnyBranches {
                strategies {
                  buildAllBranches {
                    strategies {
                      buildRegularBranches()
                      // skipInitialBuildOnFirstBranchIndexing()
                    }
                  }
                }
              }
              buildAnyBranches {
                strategies {
                  buildAllBranches {
                    strategies {
                      buildTags {
                        atLeastDays '-1'
                        atMostDays '3'
                      }
                      // skipInitialBuildOnFirstBranchIndexing()
                    }
                  }
                }
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
      triggers {
        periodicFolderTrigger {
          interval("86400000")
        }
      }
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
              buildAnyBranches {
                strategies {
                  buildAllBranches {
                    strategies {
                      buildRegularBranches()
                      // skipInitialBuildOnFirstBranchIndexing()
                    }
                  }
                }
              }
              buildAnyBranches {
                strategies {
                  buildAllBranches {
                    strategies {
                      buildTags {
                        atLeastDays '-1'
                        atMostDays '3'
                      }
                      // skipInitialBuildOnFirstBranchIndexing()
                    }
                  }
                }
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
      }
      orphanedItemStrategy {
        discardOldItems {
          numToKeep(5)
        }
      }
    }
  }
}

githubOrgs.keySet().each { slug ->
  organizationFolder(slug) {
    authorization {
      if (githubOrgs[slug]["public"]) {
        ["anonymous", "authorized", "authenticated"].each { user ->
          permission("hudson.model.Item.Read", user)
          permission("hudson.model.Item.Discover", user)
        }
      }
      if (githubOrgs[slug]["allowed_read_users"]) {
        githubOrgs[slug]["allowed_read_users"].each { user ->
          permission("hudson.model.Item.Read", user)
          permission("hudson.model.Item.Discover", user)
        }
      }
    }
    buildStrategies {
      buildAnyBranches {
        strategies {
          buildAllBranches {
            strategies {
              buildRegularBranches()
              // skipInitialBuildOnFirstBranchIndexing()
            }
          }
        }
      }
      buildAnyBranches {
        strategies {
          buildAllBranches {
            strategies {
              buildTags {
                atLeastDays '-1'
                atMostDays '3'
              }
              // skipInitialBuildOnFirstBranchIndexing()
            }
          }
        }
      }
    }
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
        credentialsId(appEnabledAccounts.contains(slug) ? "github-app-" + slug : "github-halkeye")
        repoOwner(slug)
        traits {
          pruneStaleBranchTrait()
          gitHubExcludeArchivedRepositories()
          gitHubBranchDiscovery {
            strategyId(3)
          }
          gitHubPullRequestDiscovery {
            strategyId(4)
          } 
          gitHubTagDiscovery()
          notificationContextTrait {
            contextLabel("continuous-integration/halkeye")
            typeSuffix(true)
          } 
        }
      }
    }
  }
}
