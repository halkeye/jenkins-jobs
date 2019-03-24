folder('github_projects') {
  ['nfg', 'aliaoca', 'authorized', 'authenticated'].each { user ->
    permission("Jenkins.READ", user)
    permission("hudson.model.Item.READ", user)
    permission("hudson.model.Item.DISCOVER", user)
  }
  displayName('Github Projects')
  description('All the github projects')
}

def githubProjects = [
    'halkeye/bamboohr-employee-stats',
    'halkeye/codacy-maven-plugin',
    'halkeye/flask_atlassian_connect',
    'halkeye/gavinmogan.com',
    'halkeye/get_groups',
    'halkeye/git-version-commits',
    'halkeye/go_windows_stats',
    'halkeye/graphite_scripts',
    'halkeye/halkeye-ansible',
    'halkeye/helm-repo-html',
    'halkeye/http_bouncer_client',
    'halkeye/http_bouncer_server',
    'halkeye/hubot-brain-redis-hash',
    'halkeye/hubot-confluence-search',
    'halkeye/hubot-jenkins-notifier',
    'halkeye/hubot-regex',
    'halkeye/hubot-sonarr',
    'halkeye/hubot-url-describer',
    'halkeye/infinicatr',
    'halkeye/minecraft.gavinmogan.com',
    'halkeye/proxy-s3-google-oauth',
    'halkeye/react-book-reader',
    'halkeye/release-dashboard',
    'halkeye/slack-foodee',
    'halkeye/soundboard',
    'halkeye/www-gavinmogan-com',

    'halkeye/discorse-docker-builder',
];
githubProjects.each { slug ->
  multibranchPipelineJob('github_projects/' + slug) {
    branchSources {
      github {
        scanCredentialsId('github-halkeye')
        repoOwner(slug.tokenize("/")[0])
        repository(slug.tokenize("/")[1])
      }
    }
    orphanedItemStrategy {
      discardOldItems {
        numToKeep(5)
      }
    }
  }
}
