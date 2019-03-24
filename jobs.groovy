folder('github_projects') {
  authorization {
    ['nfg', 'aliaoca', 'authorized', 'authenticated'].each { user ->
      permission("Jenkins.READ", user)
      permission("hudson.model.Item.READ", user)
      permission("hudson.model.Item.DISCOVER", user)
    }
  }
  displayName('Github Projects')
  description('All the github projects')
}


def githubProjects = new groovy.json.JsonSlurper().parse(
  (new URL("https://raw.githubusercontent.com/halkeye/jenkins-jobs/master/jobs.groovy")).newReader()
)
githubProjects.keySet().each { username ->
  githubProjects[username].each { slug ->
    multibranchPipelineJob('github_projects/' + username + '/' + slug) {
      branchSources {
        github {
          scanCredentialsId('github-halkeye')
          repoOwner(username)
          repository(slug)
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
