/*
 * Copyright (c) 2023 Unknown Network Developers and contributors.
 *
 * All rights reserved.
 *
 * NOTICE: This license is subject to change without prior notice.
 *
 * Redistribution and use in source and binary forms, *without modification*,
 *     are permitted provided that the following conditions are met:
 *
 * I. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 * II. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * III. Neither the name of Unknown Network nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 *
 * IV. This source code and binaries is provided by the copyright holders and contributors "AS-IS" and
 *     any express or implied warranties, including, but not limited to, the implied warranties of
 *     merchantability and fitness for a particular purpose are disclaimed.
 *     In not event shall the copyright owner or contributors be liable for
 *     any direct, indirect, incidental, special, exemplary, or consequential damages
 *     (including but not limited to procurement of substitute goods or services;
 *     loss of use data or profits; or business interruption) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package com.ryuuta0217.api.github.repository.commit;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.repository.commit.interfaces.Commit;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.Map;

// {
//  "base_commit": {
//    "committer": {
//      "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//      "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//      "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//      "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//      "login": "ryuuta0217",
//      "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//      "type": "User",
//      "url": "https://api.github.com/users/ryuuta0217",
//      "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//      "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//      "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//      "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//      "html_url": "https://github.com/ryuuta0217",
//      "site_admin": false,
//      "id": 33993422,
//      "gravatar_id": "",
//      "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//      "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//    },
//    "author": {
//      "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//      "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//      "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//      "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//      "login": "ryuuta0217",
//      "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//      "type": "User",
//      "url": "https://api.github.com/users/ryuuta0217",
//      "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//      "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//      "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//      "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//      "html_url": "https://github.com/ryuuta0217",
//      "site_admin": false,
//      "id": 33993422,
//      "gravatar_id": "",
//      "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//      "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//    },
//    "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7",
//    "commit": {
//      "comment_count": 0,
//      "committer": {
//        "date": "2023-06-14T06:59:32Z",
//        "name": "Ryuta Iwakura",
//        "email": "ryuuta.iwakura@gmail.com"
//      },
//      "author": {
//        "date": "2023-06-14T06:59:32Z",
//        "name": "Ryuta Iwakura",
//        "email": "ryuuta.iwakura@gmail.com"
//      },
//      "tree": {
//        "sha": "acdd8be68ae693c8e0e8d84119f64ca68fc570a7",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/trees/acdd8be68ae693c8e0e8d84119f64ca68fc570a7"
//      },
//      "message": "Merge branch 'develop' into production",
//      "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/commits/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7",
//      "verification": {
//        "reason": "unsigned",
//        "signature": null,
//        "payload": null,
//        "verified": false
//      }
//    },
//    "comments_url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7/comments",
//    "sha": "f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7",
//    "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7",
//    "node_id": "C_kwDOGi2nF9oAKGY4ZWI1NGFjNGMzZmFiOGFkMmE2ZWUwNjVjNTNjOGZkZjM2ZjYxZjc",
//    "parents": [
//      {
//        "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/fdf2a16dc8fd39fd94585a815a7366d6ef2aab08",
//        "sha": "fdf2a16dc8fd39fd94585a815a7366d6ef2aab08",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/fdf2a16dc8fd39fd94585a815a7366d6ef2aab08"
//      },
//      {
//        "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/220db7c0c4f500c7071aa1f2e7ceb34e3e2aaf62",
//        "sha": "220db7c0c4f500c7071aa1f2e7ceb34e3e2aaf62",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/220db7c0c4f500c7071aa1f2e7ceb34e3e2aaf62"
//      }
//    ]
//  },
//  "behind_by": 0,
//  "diff_url": "https://github.com/ryuuta0217/UnknownNetworkCore/compare/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7...a85edc6477d8f4b9924269fe5fb9a5eabab2df89.diff",
//  "ahead_by": 4,
//  "merge_base_commit": {
//    "committer": {
//      "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//      "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//      "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//      "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//      "login": "ryuuta0217",
//      "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//      "type": "User",
//      "url": "https://api.github.com/users/ryuuta0217",
//      "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//      "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//      "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//      "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//      "html_url": "https://github.com/ryuuta0217",
//      "site_admin": false,
//      "id": 33993422,
//      "gravatar_id": "",
//      "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//      "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//    },
//    "author": {
//      "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//      "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//      "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//      "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//      "login": "ryuuta0217",
//      "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//      "type": "User",
//      "url": "https://api.github.com/users/ryuuta0217",
//      "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//      "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//      "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//      "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//      "html_url": "https://github.com/ryuuta0217",
//      "site_admin": false,
//      "id": 33993422,
//      "gravatar_id": "",
//      "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//      "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//    },
//    "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7",
//    "commit": {
//      "comment_count": 0,
//      "committer": {
//        "date": "2023-06-14T06:59:32Z",
//        "name": "Ryuta Iwakura",
//        "email": "ryuuta.iwakura@gmail.com"
//      },
//      "author": {
//        "date": "2023-06-14T06:59:32Z",
//        "name": "Ryuta Iwakura",
//        "email": "ryuuta.iwakura@gmail.com"
//      },
//      "tree": {
//        "sha": "acdd8be68ae693c8e0e8d84119f64ca68fc570a7",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/trees/acdd8be68ae693c8e0e8d84119f64ca68fc570a7"
//      },
//      "message": "Merge branch 'develop' into production",
//      "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/commits/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7",
//      "verification": {
//        "reason": "unsigned",
//        "signature": null,
//        "payload": null,
//        "verified": false
//      }
//    },
//    "comments_url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7/comments",
//    "sha": "f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7",
//    "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7",
//    "node_id": "C_kwDOGi2nF9oAKGY4ZWI1NGFjNGMzZmFiOGFkMmE2ZWUwNjVjNTNjOGZkZjM2ZjYxZjc",
//    "parents": [
//      {
//        "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/fdf2a16dc8fd39fd94585a815a7366d6ef2aab08",
//        "sha": "fdf2a16dc8fd39fd94585a815a7366d6ef2aab08",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/fdf2a16dc8fd39fd94585a815a7366d6ef2aab08"
//      },
//      {
//        "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/220db7c0c4f500c7071aa1f2e7ceb34e3e2aaf62",
//        "sha": "220db7c0c4f500c7071aa1f2e7ceb34e3e2aaf62",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/220db7c0c4f500c7071aa1f2e7ceb34e3e2aaf62"
//      }
//    ]
//  },
//  "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/compare/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7...a85edc6477d8f4b9924269fe5fb9a5eabab2df89",
//  "total_commits": 4,
//  "patch_url": "https://github.com/ryuuta0217/UnknownNetworkCore/compare/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7...a85edc6477d8f4b9924269fe5fb9a5eabab2df89.patch",
//  "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/compare/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7...a85edc6477d8f4b9924269fe5fb9a5eabab2df89",
//  "commits": [
//    {
//      "committer": {
//        "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//        "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//        "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//        "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//        "login": "ryuuta0217",
//        "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//        "type": "User",
//        "url": "https://api.github.com/users/ryuuta0217",
//        "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//        "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//        "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//        "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//        "html_url": "https://github.com/ryuuta0217",
//        "site_admin": false,
//        "id": 33993422,
//        "gravatar_id": "",
//        "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//        "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//      },
//      "author": {
//        "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//        "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//        "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//        "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//        "login": "ryuuta0217",
//        "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//        "type": "User",
//        "url": "https://api.github.com/users/ryuuta0217",
//        "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//        "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//        "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//        "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//        "html_url": "https://github.com/ryuuta0217",
//        "site_admin": false,
//        "id": 33993422,
//        "gravatar_id": "",
//        "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//        "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//      },
//      "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/036c7146a6159dff5c70eea7b927f67653ffce7d",
//      "commit": {
//        "comment_count": 0,
//        "committer": {
//          "date": "2023-06-15T15:50:54Z",
//          "name": "Ryuta Iwakura",
//          "email": "ryuuta.iwakura@gmail.com"
//        },
//        "author": {
//          "date": "2023-06-15T15:50:54Z",
//          "name": "Ryuta Iwakura",
//          "email": "ryuuta.iwakura@gmail.com"
//        },
//        "tree": {
//          "sha": "ec6371f38d382e5adae0032d24e45fd9e132ba50",
//          "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/trees/ec6371f38d382e5adae0032d24e45fd9e132ba50"
//        },
//        "message": "Bump bootstrap version",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/commits/036c7146a6159dff5c70eea7b927f67653ffce7d",
//        "verification": {
//          "reason": "unsigned",
//          "signature": null,
//          "payload": null,
//          "verified": false
//        }
//      },
//      "comments_url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/036c7146a6159dff5c70eea7b927f67653ffce7d/comments",
//      "sha": "036c7146a6159dff5c70eea7b927f67653ffce7d",
//      "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/036c7146a6159dff5c70eea7b927f67653ffce7d",
//      "node_id": "C_kwDOGi2nF9oAKDAzNmM3MTQ2YTYxNTlkZmY1YzcwZWVhN2I5MjdmNjc2NTNmZmNlN2Q",
//      "parents": [{
//        "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/220db7c0c4f500c7071aa1f2e7ceb34e3e2aaf62",
//        "sha": "220db7c0c4f500c7071aa1f2e7ceb34e3e2aaf62",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/220db7c0c4f500c7071aa1f2e7ceb34e3e2aaf62"
//      }]
//    },
//    {
//      "committer": {
//        "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//        "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//        "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//        "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//        "login": "ryuuta0217",
//        "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//        "type": "User",
//        "url": "https://api.github.com/users/ryuuta0217",
//        "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//        "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//        "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//        "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//        "html_url": "https://github.com/ryuuta0217",
//        "site_admin": false,
//        "id": 33993422,
//        "gravatar_id": "",
//        "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//        "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//      },
//      "author": {
//        "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//        "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//        "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//        "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//        "login": "ryuuta0217",
//        "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//        "type": "User",
//        "url": "https://api.github.com/users/ryuuta0217",
//        "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//        "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//        "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//        "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//        "html_url": "https://github.com/ryuuta0217",
//        "site_admin": false,
//        "id": 33993422,
//        "gravatar_id": "",
//        "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//        "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//      },
//      "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/093ba597789adbb233bcc73cc88bbbe44695b7d7",
//      "commit": {
//        "comment_count": 0,
//        "committer": {
//          "date": "2023-06-15T15:51:08Z",
//          "name": "Ryuta Iwakura",
//          "email": "ryuuta.iwakura@gmail.com"
//        },
//        "author": {
//          "date": "2023-06-15T15:51:08Z",
//          "name": "Ryuta Iwakura",
//          "email": "ryuuta.iwakura@gmail.com"
//        },
//        "tree": {
//          "sha": "c8f70025c92825b236c29ec5f3aba0710db39460",
//          "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/trees/c8f70025c92825b236c29ec5f3aba0710db39460"
//        },
//        "message": "feat: ChestLinkStick",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/commits/093ba597789adbb233bcc73cc88bbbe44695b7d7",
//        "verification": {
//          "reason": "unsigned",
//          "signature": null,
//          "payload": null,
//          "verified": false
//        }
//      },
//      "comments_url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/093ba597789adbb233bcc73cc88bbbe44695b7d7/comments",
//      "sha": "093ba597789adbb233bcc73cc88bbbe44695b7d7",
//      "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/093ba597789adbb233bcc73cc88bbbe44695b7d7",
//      "node_id": "C_kwDOGi2nF9oAKDA5M2JhNTk3Nzg5YWRiYjIzM2JjYzczY2M4OGJiYmU0NDY5NWI3ZDc",
//      "parents": [{
//        "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/036c7146a6159dff5c70eea7b927f67653ffce7d",
//        "sha": "036c7146a6159dff5c70eea7b927f67653ffce7d",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/036c7146a6159dff5c70eea7b927f67653ffce7d"
//      }]
//    },
//    {
//      "committer": {
//        "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//        "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//        "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//        "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//        "login": "ryuuta0217",
//        "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//        "type": "User",
//        "url": "https://api.github.com/users/ryuuta0217",
//        "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//        "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//        "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//        "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//        "html_url": "https://github.com/ryuuta0217",
//        "site_admin": false,
//        "id": 33993422,
//        "gravatar_id": "",
//        "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//        "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//      },
//      "author": {
//        "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//        "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//        "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//        "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//        "login": "ryuuta0217",
//        "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//        "type": "User",
//        "url": "https://api.github.com/users/ryuuta0217",
//        "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//        "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//        "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//        "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//        "html_url": "https://github.com/ryuuta0217",
//        "site_admin": false,
//        "id": 33993422,
//        "gravatar_id": "",
//        "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//        "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//      },
//      "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/6a1a6c6d7939de645ad2ceda1733e273b0d09ab8",
//      "commit": {
//        "comment_count": 0,
//        "committer": {
//          "date": "2023-06-15T15:51:58Z",
//          "name": "Ryuta Iwakura",
//          "email": "ryuuta.iwakura@gmail.com"
//        },
//        "author": {
//          "date": "2023-06-15T15:51:58Z",
//          "name": "Ryuta Iwakura",
//          "email": "ryuuta.iwakura@gmail.com"
//        },
//        "tree": {
//          "sha": "ccdbe069391b28e12e6b8102607059820811af38",
//          "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/trees/ccdbe069391b28e12e6b8102607059820811af38"
//        },
//        "message": "HopperGui: Re:worked",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/commits/6a1a6c6d7939de645ad2ceda1733e273b0d09ab8",
//        "verification": {
//          "reason": "unsigned",
//          "signature": null,
//          "payload": null,
//          "verified": false
//        }
//      },
//      "comments_url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/6a1a6c6d7939de645ad2ceda1733e273b0d09ab8/comments",
//      "sha": "6a1a6c6d7939de645ad2ceda1733e273b0d09ab8",
//      "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/6a1a6c6d7939de645ad2ceda1733e273b0d09ab8",
//      "node_id": "C_kwDOGi2nF9oAKDZhMWE2YzZkNzkzOWRlNjQ1YWQyY2VkYTE3MzNlMjczYjBkMDlhYjg",
//      "parents": [{
//        "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/093ba597789adbb233bcc73cc88bbbe44695b7d7",
//        "sha": "093ba597789adbb233bcc73cc88bbbe44695b7d7",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/093ba597789adbb233bcc73cc88bbbe44695b7d7"
//      }]
//    },
//    {
//      "committer": {
//        "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//        "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//        "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//        "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//        "login": "ryuuta0217",
//        "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//        "type": "User",
//        "url": "https://api.github.com/users/ryuuta0217",
//        "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//        "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//        "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//        "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//        "html_url": "https://github.com/ryuuta0217",
//        "site_admin": false,
//        "id": 33993422,
//        "gravatar_id": "",
//        "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//        "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//      },
//      "author": {
//        "gists_url": "https://api.github.com/users/ryuuta0217/gists{/gist_id}",
//        "repos_url": "https://api.github.com/users/ryuuta0217/repos",
//        "following_url": "https://api.github.com/users/ryuuta0217/following{/other_user}",
//        "starred_url": "https://api.github.com/users/ryuuta0217/starred{/owner}{/repo}",
//        "login": "ryuuta0217",
//        "followers_url": "https://api.github.com/users/ryuuta0217/followers",
//        "type": "User",
//        "url": "https://api.github.com/users/ryuuta0217",
//        "subscriptions_url": "https://api.github.com/users/ryuuta0217/subscriptions",
//        "received_events_url": "https://api.github.com/users/ryuuta0217/received_events",
//        "avatar_url": "https://avatars.githubusercontent.com/u/33993422?v=4",
//        "events_url": "https://api.github.com/users/ryuuta0217/events{/privacy}",
//        "html_url": "https://github.com/ryuuta0217",
//        "site_admin": false,
//        "id": 33993422,
//        "gravatar_id": "",
//        "node_id": "MDQ6VXNlcjMzOTkzNDIy",
//        "organizations_url": "https://api.github.com/users/ryuuta0217/orgs"
//      },
//      "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/a85edc6477d8f4b9924269fe5fb9a5eabab2df89",
//      "commit": {
//        "comment_count": 0,
//        "committer": {
//          "date": "2023-06-15T15:57:07Z",
//          "name": "Ryuta Iwakura",
//          "email": "ryuuta.iwakura@gmail.com"
//        },
//        "author": {
//          "date": "2023-06-15T15:57:07Z",
//          "name": "Ryuta Iwakura",
//          "email": "ryuuta.iwakura@gmail.com"
//        },
//        "tree": {
//          "sha": "ccdbe069391b28e12e6b8102607059820811af38",
//          "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/trees/ccdbe069391b28e12e6b8102607059820811af38"
//        },
//        "message": "Merge branch 'develop' into production",
//        "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/git/commits/a85edc6477d8f4b9924269fe5fb9a5eabab2df89",
//        "verification": {
//          "reason": "unsigned",
//          "signature": null,
//          "payload": null,
//          "verified": false
//        }
//      },
//      "comments_url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/a85edc6477d8f4b9924269fe5fb9a5eabab2df89/comments",
//      "sha": "a85edc6477d8f4b9924269fe5fb9a5eabab2df89",
//      "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/a85edc6477d8f4b9924269fe5fb9a5eabab2df89",
//      "node_id": "C_kwDOGi2nF9oAKGE4NWVkYzY0NzdkOGY0Yjk5MjQyNjlmZTVmYjlhNWVhYmFiMmRmODk",
//      "parents": [
//        {
//          "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7",
//          "sha": "f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7",
//          "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/f8eb54ac4c3fab8ad2a6ee065c53c8fdf36f61f7"
//        },
//        {
//          "html_url": "https://github.com/ryuuta0217/UnknownNetworkCore/commit/6a1a6c6d7939de645ad2ceda1733e273b0d09ab8",
//          "sha": "6a1a6c6d7939de645ad2ceda1733e273b0d09ab8",
//          "url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/commits/6a1a6c6d7939de645ad2ceda1733e273b0d09ab8"
//        }
//      ]
//    }
//  ],
//  "files": [
//    {
//      "patch": "@@ -61,8 +61,8 @@ dependencies {\n     compileOnly group: 'com.sk89q.worldguard', name: 'worldguard-bukkit', version: '+'\n     compileOnly group: 'com.github.MilkBowl', name: 'VaultAPI', version: '+'\n     compileOnly group: 'net.luckperms', name: 'api', version: '+'\n-    compileOnly group: 'net.unknown', name: 'UnknownNetworkBootstrap', version: 'fbd74c08'\n-    launchwrapper group: 'net.unknown', name: 'UnknownNetworkBootstrap', version: 'fbd74c08', classifier: 'spigot'\n+    compileOnly group: 'net.unknown', name: 'UnknownNetworkBootstrap', version: '204d13e1'\n+    launchwrapper group: 'net.unknown', name: 'UnknownNetworkBootstrap', version: '204d13e1', classifier: 'spigot'\n     implementation group: 'org.mozilla', name: 'rhino', version: '+'\n     compileOnly group: 'com.github.NuVotifier', name: 'NuVotifier', version: '+'\n     implementation group: 'net.dv8tion', name: 'JDA', version: '+'",
//      "filename": "build.gradle",
//      "additions": 2,
//      "deletions": 2,
//      "changes": 4,
//      "sha": "dd861a56a7d7b301b1dbd43e09216dc1f69f5072",
//      "blob_url": "https://github.com/ryuuta0217/UnknownNetworkCore/blob/a85edc6477d8f4b9924269fe5fb9a5eabab2df89/build.gradle",
//      "raw_url": "https://github.com/ryuuta0217/UnknownNetworkCore/raw/a85edc6477d8f4b9924269fe5fb9a5eabab2df89/build.gradle",
//      "status": "modified",
//      "contents_url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/contents/build.gradle?ref=a85edc6477d8f4b9924269fe5fb9a5eabab2df89"
//    },
//    {
//      "patch": "@@ -47,10 +47,7 @@\n import net.unknown.survival.enchants.*;\n import net.unknown.survival.enchants.nms.DamageEnchant;\n import net.unknown.survival.events.ModifiableBlockBreakEvent;\n-import net.unknown.survival.feature.BlockDisassembler;\n-import net.unknown.survival.feature.DebugStickEntityEditor;\n-import net.unknown.survival.feature.ProtectedAreaTestStick;\n-import net.unknown.survival.feature.VoteListener;\n+import net.unknown.survival.feature.*;\n import net.unknown.survival.feature.gnarms.GNArms;\n import net.unknown.survival.fml.FMLConnectionListener;\n import net.unknown.survival.fml.ModdedPlayerManager;\n@@ -127,6 +124,7 @@ public static void onEnable() {\n             getLogger().info(\"Successfully Bootstrapped!\");\n             ListenerManager.registerListener(new BlockDisassembler());\n             ListenerManager.registerListener(new ConfigureHopperGui.Listener());\n+            ListenerManager.registerListener(new ChestLinkStick());\n         }\n \n         Bukkit.getMessenger().registerOutgoingPluginChannel(UnknownNetworkCore.getInstance(), \"BungeeCord\");",
//      "filename": "src/main/java/net/unknown/survival/UnknownNetworkSurvival.java",
//      "additions": 2,
//      "deletions": 4,
//      "changes": 6,
//      "sha": "9597658142f2cd3e69860b1b7df8aceb7372275f",
//      "blob_url": "https://github.com/ryuuta0217/UnknownNetworkCore/blob/a85edc6477d8f4b9924269fe5fb9a5eabab2df89/src%2Fmain%2Fjava%2Fnet%2Funknown%2Fsurvival%2FUnknownNetworkSurvival.java",
//      "raw_url": "https://github.com/ryuuta0217/UnknownNetworkCore/raw/a85edc6477d8f4b9924269fe5fb9a5eabab2df89/src%2Fmain%2Fjava%2Fnet%2Funknown%2Fsurvival%2FUnknownNetworkSurvival.java",
//      "status": "modified",
//      "contents_url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/contents/src%2Fmain%2Fjava%2Fnet%2Funknown%2Fsurvival%2FUnknownNetworkSurvival.java?ref=a85edc6477d8f4b9924269fe5fb9a5eabab2df89"
//    },
//    {
//      "patch": "@@ -0,0 +1,295 @@\n+/*\n+ * Copyright (c) 2023 Unknown Network Developers and contributors.\n+ *\n+ * All rights reserved.\n+ *\n+ * NOTICE: This license is subject to change without prior notice.\n+ *\n+ * Redistribution and use in source and binary forms, *without modification*,\n+ *     are permitted provided that the following conditions are met:\n+ *\n+ * I. Redistributions of source code must retain the above copyright notice,\n+ *     this list of conditions and the following disclaimer.\n+ *\n+ * II. Redistributions in binary form must reproduce the above copyright notice,\n+ *     this list of conditions and the following disclaimer in the\n+ *     documentation and/or other materials provided with the distribution.\n+ *\n+ * III. Neither the name of Unknown Network nor the names of its contributors may be used to\n+ *     endorse or promote products derived from this software without specific prior written permission.\n+ *\n+ * IV. This source code and binaries is provided by the copyright holders and contributors \"AS-IS\" and\n+ *     any express or implied warranties, including, but not limited to, the implied warranties of\n+ *     merchantability and fitness for a particular purpose are disclaimed.\n+ *     In not event shall the copyright owner or contributors be liable for\n+ *     any direct, indirect, incidental, special, exemplary, or consequential damages\n+ *     (including but not limited to procurement of substitute goods or services;\n+ *     loss of use data or profits; or business interruption) however caused and on any theory of liability,\n+ *     whether in contract, strict liability, or tort (including negligence or otherwise)\n+ *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.\n+ */\n+\n+package net.unknown.survival.feature;\n+\n+import kotlin.Suppress;\n+import net.kyori.adventure.text.Component;\n+import net.kyori.adventure.text.format.Style;\n+import net.kyori.adventure.text.format.TextDecoration;\n+import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;\n+import net.minecraft.world.level.block.entity.BlockEntity;\n+import net.unknown.UnknownNetworkCore;\n+import net.unknown.core.define.DefinedTextColor;\n+import net.unknown.core.util.MessageUtil;\n+import net.unknown.core.util.MinecraftAdapter;\n+import net.unknown.core.util.NewMessageUtil;\n+import net.unknown.launchwrapper.linkchest.LinkChestMode;\n+import net.unknown.launchwrapper.mixininterfaces.IMixinChestBlockEntity;\n+import org.bukkit.Bukkit;\n+import org.bukkit.Location;\n+import org.bukkit.Material;\n+import org.bukkit.block.Block;\n+import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;\n+import org.bukkit.entity.Player;\n+import org.bukkit.event.EventHandler;\n+import org.bukkit.event.Listener;\n+import org.bukkit.event.block.Action;\n+import org.bukkit.event.player.PlayerInteractEvent;\n+import org.bukkit.event.player.PlayerItemHeldEvent;\n+import org.bukkit.inventory.EquipmentSlot;\n+import org.bukkit.inventory.ItemStack;\n+import org.bukkit.inventory.meta.ItemMeta;\n+\n+import javax.annotation.Nullable;\n+import java.util.ArrayList;\n+import java.util.List;\n+import java.util.Objects;\n+\n+\n+public class ChestLinkStick implements Listener {\n+    @EventHandler\n+    public void onPlayerInteract(PlayerInteractEvent event) {\n+        if (event.getHand() != EquipmentSlot.HAND) return;\n+        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;\n+        Block clickedBlock = event.getClickedBlock();\n+        if (clickedBlock == null || clickedBlock.getType() != Material.CHEST) return;\n+        ItemStack handItem = event.getItem();\n+        if (handItem == null || !isChestLinkStick(event.getItem())) return;\n+        event.setCancelled(true);\n+\n+        StickMode stickMode = getStickMode(handItem);\n+        if (stickMode == null) return;\n+\n+        switch (stickMode) {\n+            case SET_SOURCE -> setSource(event.getPlayer(), handItem, clickedBlock.getLocation());\n+            case ADD_CLIENT -> addClient(event.getPlayer(), handItem, clickedBlock.getLocation());\n+            case REMOVE_CLIENT -> removeClient(event.getPlayer(), handItem, clickedBlock.getLocation());\n+        }\n+    }\n+\n+    @EventHandler\n+    public void onPlayerItemHeld(PlayerItemHeldEvent event) {\n+        if (!event.getPlayer().isSneaking()) return;\n+        if (event.getPreviousSlot() == event.getNewSlot()) return;\n+        ItemStack heldItem = event.getPlayer().getInventory().getItem(event.getPreviousSlot());\n+        if (!isChestLinkStick(heldItem)) return;\n+        event.setCancelled(true);\n+        switchStickMode(event.getPlayer(), heldItem);\n+    }\n+\n+    private static boolean isChestLinkStick(ItemStack stack) {\n+        return stack != null && stack.getType() == Material.STICK && stack.hasItemMeta() && Component.text(\"チェストリンクの杖\", Style.style(DefinedTextColor.GREEN, TextDecoration.BOLD)).decoration(TextDecoration.ITALIC, false).equals(stack.getItemMeta().displayName()) && stack.getItemMeta().hasLore();\n+    }\n+\n+    private static boolean hasStickMode(ItemStack stack) {\n+        if (!isChestLinkStick(stack)) return false;\n+        if (!stack.hasItemMeta()) return false;\n+        ItemMeta meta = stack.getItemMeta();\n+        if (!meta.hasLore()) return false;\n+        return meta.lore().stream().anyMatch(lore -> PlainTextComponentSerializer.plainText().serialize(lore).startsWith(\"リンクモード: \"));\n+    }\n+\n+    private static StickMode switchStickMode(Player player, ItemStack stack) {\n+        StickMode current = getStickMode(stack);\n+        if (current == null) current = StickMode.SET_SOURCE;\n+        StickMode next = StickMode.values()[(current.ordinal() + 1) % StickMode.values().length];\n+        updateStickMode(player, stack, next);\n+        return next;\n+    }\n+\n+    private static void updateStickMode(Player player, ItemStack stack, StickMode newMode) {\n+        if (!stack.hasItemMeta()) return;\n+        ItemMeta meta = stack.getItemMeta();\n+\n+        Component loreLine = Component.text(\"リンクモード: \" + newMode.getModeName(), DefinedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);\n+\n+        if (hasStickMode(stack)) {\n+            meta.lore(meta.lore().stream().map(lore -> {\n+                if (PlainTextComponentSerializer.plainText().serialize(lore).startsWith(\"リンクモード: \")) return loreLine;\n+                return lore;\n+            }).toList());\n+        } else {\n+            meta.lore(new ArrayList<>() {{\n+                if (meta.hasLore()) addAll(meta.lore());\n+                add(loreLine);\n+            }});\n+        }\n+        stack.setItemMeta(meta);\n+\n+        player.sendActionBar(Component.empty()\n+                .append(stack.displayName())\n+                .appendSpace()\n+                .append(Component.text(\"のモードを切り替えました:\"))\n+                .appendSpace()\n+                .append(newMode.getDisplayName()));\n+    }\n+\n+    private static StickMode getStickMode(ItemStack stick) {\n+        if (!stick.hasItemMeta()) return null;\n+        if (!stick.getItemMeta().hasLore()) return null;\n+        return stick.getItemMeta().lore().stream()\n+                .map(PlainTextComponentSerializer.plainText()::serialize)\n+                .filter(s -> s.startsWith(\"リンクモード: \"))\n+                .map(s -> s.split(\": ?\", 2)[1])\n+                .map(StickMode::fromModeName)\n+                .filter(Objects::nonNull)\n+                .findFirst().orElse(null);\n+    }\n+\n+    private static boolean hasSource(ItemStack stack) {\n+        if (!stack.hasItemMeta()) return false;\n+        if (!stack.getItemMeta().hasLore()) return false;\n+        return stack.getItemMeta().lore().stream().anyMatch(lore -> PlainTextComponentSerializer.plainText().serialize(lore).startsWith(\"ソース: \"));\n+    }\n+\n+    private static void setSource(Player player, ItemStack stack, @Nullable Location sourcePos) {\n+        if (!stack.hasItemMeta()) return;\n+        ItemMeta meta = stack.getItemMeta();\n+        if (!meta.hasLore()) return;\n+\n+        if (sourcePos != null) {\n+            IMixinChestBlockEntity chestBlockEntity = getChestBlockEntity(sourcePos);\n+            if (chestBlockEntity != null) {\n+                chestBlockEntity.setChestTransportMode(LinkChestMode.SOURCE);\n+\n+                Component sourceLore = Component.text(String.format(\"ソース: %s, %s, %s, %s\", sourcePos.getWorld().getName(), sourcePos.getBlockX(), sourcePos.getBlockY(), sourcePos.getBlockZ()), DefinedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);\n+\n+                if (hasSource(stack)) {\n+                    meta.lore(meta.lore().stream().map(lore -> {\n+                        if (PlainTextComponentSerializer.plainText().serialize(lore).startsWith(\"ソース: \"))\n+                            return sourceLore;\n+                        return lore;\n+                    }).toList());\n+                } else {\n+                    meta.lore(new ArrayList<>() {{\n+                        addAll(meta.lore());\n+                        add(sourceLore);\n+                    }});\n+                }\n+                stack.setItemMeta(meta);\n+                NewMessageUtil.sendMessage(player, Component.text(\"ソースとなるチェストを\" + MessageUtil.getWorldName(sourcePos.getWorld()) + \",\" + sourcePos.getBlockX() + \",\" + sourcePos.getBlockY() + \",\" + sourcePos.getBlockZ() + \"に設定しました\"));\n+                updateStickMode(player, stack, StickMode.ADD_CLIENT);\n+            } else {\n+                NewMessageUtil.sendErrorMessage(player, Component.text(\"クリックされたチェストは IMixinChestBlockEntity ではありませんでした。これはバグの可能性があります。\"));\n+            }\n+        } else {\n+            if (meta.hasLore()) {\n+                List<Component> lore = meta.lore();\n+                if (lore != null) {\n+                    lore.removeIf(line -> PlainTextComponentSerializer.plainText().serialize(line).startsWith(\"ソース: \"));\n+                    meta.lore(lore);\n+                    stack.setItemMeta(meta);\n+                }\n+            }\n+        }\n+    }\n+\n+    private static void addClient(Player player, ItemStack stack, Location clientPos) {\n+        if (!isChestLinkStick(stack)) return;\n+\n+        IMixinChestBlockEntity chestBlockEntity = getChestBlockEntity(clientPos);\n+        if (chestBlockEntity != null) {\n+            Location sourcePos = getSource(stack);\n+            if (sourcePos != null) {\n+                IMixinChestBlockEntity sourceChestBlockEntity = getChestBlockEntity(sourcePos);\n+                if (sourceChestBlockEntity != null && sourceChestBlockEntity.getChestTransportMode() == LinkChestMode.SOURCE) {\n+                    chestBlockEntity.setChestTransportMode(LinkChestMode.CLIENT);\n+                    chestBlockEntity.setLinkSource(MinecraftAdapter.level(sourcePos.getWorld()), MinecraftAdapter.blockPos(sourcePos));\n+\n+                    NewMessageUtil.sendMessage(player, Component.text(MessageUtil.getWorldName(clientPos.getWorld()) + \",\" + clientPos.getBlockX() + \",\" + clientPos.getBlockY() + \",\" + clientPos.getBlockZ() + \"のチェストをクライアントとして設定しました\"));\n+                } else {\n+                    NewMessageUtil.sendErrorMessage(player, Component.text(\"ソースとして設定されている座標にチェストが見つかりませんでした。先にソースとなるチェストを設定してください。\"));\n+                    setSource(player, stack, null);\n+                    updateStickMode(player, stack, StickMode.SET_SOURCE);\n+                }\n+            } else {\n+                NewMessageUtil.sendErrorMessage(player, Component.text(\"ソースとなるチェストが設定されていません。先にソースとなるチェストを設定してください。\"));\n+                updateStickMode(player, stack, StickMode.SET_SOURCE);\n+            }\n+        } else {\n+            NewMessageUtil.sendErrorMessage(player, Component.text(\"クリックされたチェストは IMixinChestBlockEntity ではありませんでした。これはバグの可能性があります。\"));\n+        }\n+    }\n+\n+    private static void removeClient(Player player, ItemStack stack, Location clientPos) {\n+        if (!isChestLinkStick(stack)) return;\n+\n+        IMixinChestBlockEntity chestBlockEntity = getChestBlockEntity(clientPos);\n+        if (chestBlockEntity != null) {\n+            if (chestBlockEntity.getChestTransportMode() == LinkChestMode.CLIENT && chestBlockEntity.getLinkSource() != null) {\n+                chestBlockEntity.setChestTransportMode(LinkChestMode.DISABLED);\n+                NewMessageUtil.sendMessage(player, Component.text(MessageUtil.getWorldName(clientPos.getWorld()) + \",\" + clientPos.getBlockX() + \",\" + clientPos.getBlockY() + \",\" + clientPos.getBlockZ() + \"のクライアントを解除しました\"));\n+            } else {\n+                NewMessageUtil.sendErrorMessage(player, Component.text(\"クリックされたチェストはクライアントとして設定されていません。\"));\n+            }\n+        } else {\n+            NewMessageUtil.sendErrorMessage(player, Component.text(\"クリックされたチェストは IMixinChestBlockEntity ではありませんでした。これはバグの可能性があります。\"));\n+        }\n+    }\n+\n+    private static IMixinChestBlockEntity getChestBlockEntity(Location pos) {\n+        BlockEntity blockEntity = MinecraftAdapter.level(pos.getWorld()).getBlockEntity(MinecraftAdapter.blockPos(pos));\n+        if (!(blockEntity instanceof IMixinChestBlockEntity chestBlockEntity)) return null;\n+        return chestBlockEntity;\n+    }\n+\n+    private static Location getSource(ItemStack stack) {\n+        if (!hasSource(stack)) return null;\n+        return stack.getItemMeta().lore().stream()\n+                .map(PlainTextComponentSerializer.plainText()::serialize)\n+                .filter(s -> s.startsWith(\"ソース: \"))\n+                .map(s -> s.split(\": ?\", 2)[1])\n+                .map(s -> s.split(\", ?\", 4))\n+                .filter(s -> s.length == 4)\n+                .map(s -> new Location(Bukkit.getWorld(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3])))\n+                .findFirst().orElse(null);\n+    }\n+\n+    private enum StickMode {\n+        SET_SOURCE(\"ソース設定\", Component.text(\"ソースの設定\", DefinedTextColor.YELLOW)),\n+        ADD_CLIENT(\"クライアント追加\", Component.text(\"クライアントの追加\", DefinedTextColor.GREEN)),\n+        REMOVE_CLIENT(\"クライアント削除\", Component.text(\"クライアントの削除\", DefinedTextColor.RED));\n+\n+        private final String modeName;\n+        private final Component displayName;\n+\n+        StickMode(String modeName, Component displayName) {\n+            this.modeName = modeName;\n+            this.displayName = displayName;\n+        }\n+\n+        public String getModeName() {\n+            return this.modeName;\n+        }\n+\n+        public Component getDisplayName() {\n+            return this.displayName;\n+        }\n+\n+        public static StickMode fromModeName(String modeName) {\n+            for (StickMode mode : StickMode.values()) {\n+                if (mode.getModeName().equals(modeName)) return mode;\n+            }\n+            return null;\n+        }\n+    }\n+}",
//      "filename": "src/main/java/net/unknown/survival/feature/ChestLinkStick.java",
//      "additions": 295,
//      "deletions": 0,
//      "changes": 295,
//      "sha": "be3fac61cdd5386b401512510b5e0a8faf1f7bea",
//      "blob_url": "https://github.com/ryuuta0217/UnknownNetworkCore/blob/a85edc6477d8f4b9924269fe5fb9a5eabab2df89/src%2Fmain%2Fjava%2Fnet%2Funknown%2Fsurvival%2Ffeature%2FChestLinkStick.java",
//      "raw_url": "https://github.com/ryuuta0217/UnknownNetworkCore/raw/a85edc6477d8f4b9924269fe5fb9a5eabab2df89/src%2Fmain%2Fjava%2Fnet%2Funknown%2Fsurvival%2Ffeature%2FChestLinkStick.java",
//      "status": "added",
//      "contents_url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/contents/src%2Fmain%2Fjava%2Fnet%2Funknown%2Fsurvival%2Ffeature%2FChestLinkStick.java?ref=a85edc6477d8f4b9924269fe5fb9a5eabab2df89"
//    },
//    {
//      "patch": "@@ -31,28 +31,47 @@\n \n package net.unknown.survival.gui.hopper.view;\n \n+import net.minecraft.core.Holder;\n+import net.minecraft.core.registries.BuiltInRegistries;\n+import net.minecraft.core.registries.Registries;\n+import net.minecraft.world.item.Item;\n import net.minecraft.world.item.ItemStack;\n+import net.minecraft.world.item.Items;\n import net.unknown.core.gui.view.PaginationView;\n import net.unknown.core.util.MinecraftAdapter;\n import net.unknown.core.util.NewMessageUtil;\n+import net.unknown.launchwrapper.hopper.Filter;\n import net.unknown.launchwrapper.hopper.ItemFilter;\n+import net.unknown.launchwrapper.hopper.TagFilter;\n import net.unknown.survival.gui.hopper.ConfigureHopperGui;\n import org.bukkit.event.inventory.InventoryClickEvent;\n \n-public class ConfigureHopperFilterManageView extends PaginationView<ItemFilter, ConfigureHopperGui> {\n+import java.util.Iterator;\n+\n+public class ConfigureHopperFilterManageView extends PaginationView<Filter, ConfigureHopperGui> {\n     private final ConfigureHopperViewBase parentView;\n \n     public ConfigureHopperFilterManageView(ConfigureHopperViewBase parentView) {\n         super(parentView.getGui(), parentView.getGui().getMixinHopper().getFilters(), (filter) -> {\n-            ItemStack viewItem = new ItemStack(filter.item());\n-            if (filter.tag() != null) viewItem.setTag(filter.tag());\n+            ItemStack viewItem = ItemStack.EMPTY;\n+            if (filter instanceof ItemFilter itemFilter) {\n+                viewItem = new ItemStack(itemFilter.getItem());\n+                if (itemFilter.getNbt() != null) viewItem.setTag(itemFilter.getNbt());\n+            } else if (filter instanceof TagFilter tagFilter) {\n+                Holder<Item> taggedFirstItem = BuiltInRegistries.ITEM.wrapAsHolder(Items.AIR);\n+                Iterable<Holder<Item>> taggedItems = BuiltInRegistries.ITEM.getTagOrEmpty(tagFilter.getTag());\n+                Iterator<Holder<Item>> taggedItemsIterator = taggedItems.iterator();\n+                if (taggedItemsIterator.hasNext()) taggedFirstItem = taggedItemsIterator.next();\n+                viewItem = new ItemStack(taggedFirstItem);\n+                if (tagFilter.getNbt() != null) viewItem.setTag(tagFilter.getNbt());\n+            }\n             return MinecraftAdapter.ItemStack.itemStack(viewItem);\n         }, true, true);\n         this.parentView = parentView;\n     }\n \n     @Override\n-    public void onElementButtonClicked(InventoryClickEvent event, ItemFilter filter) {\n+    public void onElementButtonClicked(InventoryClickEvent event, Filter filter) {\n         switch (event.getClick()) {\n             case SHIFT_RIGHT -> {\n                 parentView.getGui().getMixinHopper().getFilters().remove(filter);",
//      "filename": "src/main/java/net/unknown/survival/gui/hopper/view/ConfigureHopperFilterManageView.java",
//      "additions": 23,
//      "deletions": 4,
//      "changes": 27,
//      "sha": "5f71cb6fe850a584eeadd8c4f9069841da9575d3",
//      "blob_url": "https://github.com/ryuuta0217/UnknownNetworkCore/blob/a85edc6477d8f4b9924269fe5fb9a5eabab2df89/src%2Fmain%2Fjava%2Fnet%2Funknown%2Fsurvival%2Fgui%2Fhopper%2Fview%2FConfigureHopperFilterManageView.java",
//      "raw_url": "https://github.com/ryuuta0217/UnknownNetworkCore/raw/a85edc6477d8f4b9924269fe5fb9a5eabab2df89/src%2Fmain%2Fjava%2Fnet%2Funknown%2Fsurvival%2Fgui%2Fhopper%2Fview%2FConfigureHopperFilterManageView.java",
//      "status": "modified",
//      "contents_url": "https://api.github.com/repos/ryuuta0217/UnknownNetworkCore/contents/src%2Fmain%2Fjava%2Fnet%2Funknown%2Fsurvival%2Fgui%2Fhopper%2Fview%2FConfigureHopperFilterManageView.java?ref=a85edc6477d8f4b9924269fe5fb9a5eabab2df89"
//    }
//  ],
//  "permalink_url": "https://github.com/ryuuta0217/UnknownNetworkCore/compare/ryuuta0217:f8eb54a...ryuuta0217:a85edc6",
//  "status": "ahead"
//}
public class CompareResult {
    private final GitHubAPI api;

    private final String url;
    private final String htmlUrl;
    private final String permalinkUrl;
    private final String diffUrl;
    private final String patchUrl;
    private final CommitImpl baseCommit;
    private final CommitImpl mergeBaseCommit;
    private final CompareStatus status;
    private final long aheadBy;
    private final long behindBy;
    private final long totalCommits;
    private final Commit[] commits;
    @Nullable
    private final File[] files;

    public CompareResult(GitHubAPI api, RepositoryMinimal repository, JSONObject data) {
        this.api = api;

        this.url = data.getString("url");
        this.htmlUrl = data.getString("html_url");
        this.permalinkUrl = data.getString("permalink_url");
        this.diffUrl = data.getString("diff_url");
        this.patchUrl = data.getString("patch_url");
        this.baseCommit = new CommitImpl(api, repository, data.getJSONObject("base_commit"));
        this.mergeBaseCommit = new CommitImpl(api, repository, data.getJSONObject("merge_base_commit"));
        this.status = CompareStatus.valueOf(data.getString("status").toUpperCase());
        this.aheadBy = data.getLong("ahead_by");
        this.behindBy = data.getLong("behind_by");
        this.totalCommits = data.getLong("total_commits");
        this.commits = data.getJSONArray("commits").toList().stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> ((Map<?, ?>) raw))
                .map(map -> new JSONObject(map))
                .map(json -> new CommitImpl(api, repository, json))
                .toArray(CommitImpl[]::new);
        this.files = data.has("files") ? data.getJSONArray("files").toList().stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> ((Map<?, ?>) raw))
                .map(map -> new JSONObject(map))
                .map(json -> new File(api, json))
                .toArray(File[]::new) : null;
    }

    public String getUrl() {
        return this.url;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public String getPermalinkUrl() {
        return this.permalinkUrl;
    }

    public String getDiffUrl() {
        return this.diffUrl;
    }

    public String getPatchUrl() {
        return this.patchUrl;
    }

    public CommitImpl getBaseCommit() {
        return this.baseCommit;
    }

    public CommitImpl getMergeBaseCommit() {
        return this.mergeBaseCommit;
    }

    public CompareStatus getStatus() {
        return this.status;
    }

    public long getAheadBy() {
        return this.aheadBy;
    }

    public long getBehindBy() {
        return this.behindBy;
    }

    public long getTotalCommits() {
        return this.totalCommits;
    }

    public Commit[] getCommits() {
        return this.commits;
    }

    @Nullable
    public File[] getFiles() {
        return this.files;
    }
}
