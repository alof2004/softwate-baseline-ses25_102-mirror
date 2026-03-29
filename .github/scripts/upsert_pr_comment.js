const fs = require('fs');

module.exports = async function upsertPrComment({ github, context, bodyPath, marker }) {
  const body = fs.readFileSync(bodyPath, 'utf8');
  const { owner, repo } = context.repo;
  const issue_number = context.issue.number;

  const { data: comments } = await github.rest.issues.listComments({
    owner,
    repo,
    issue_number,
    per_page: 100,
  });

  const existing = comments.find(
    (comment) => comment.user?.type === 'Bot' && comment.body?.includes(marker),
  );

  if (existing) {
    await github.rest.issues.updateComment({
      owner,
      repo,
      comment_id: existing.id,
      body,
    });
    return;
  }

  await github.rest.issues.createComment({
    owner,
    repo,
    issue_number,
    body,
  });
};
