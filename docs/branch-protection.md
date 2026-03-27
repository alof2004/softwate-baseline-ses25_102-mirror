# Branch Protection Setup

This guide walks you through configuring branch protection rules to enforce SAST checks before merging PRs to the `master` branch.

## Prerequisites

⚠️ **IMPORTANT**: The workflow files must exist on the `master` branch before you can use them for branch protection.

**First time setup?** See: **[docs/bootstrap-workflow.md](bootstrap-workflow.md)**

That guide explains how to get the workflows onto `master` for the first time (it's a chicken-and-egg problem).

**Already have workflows on master?** Continue with this guide below.

## Overview

**Git Workflow:**
- Development happens on `develop` branch (or feature branches)
- PRs are created FROM `develop` TO `master`
- `master` is the protected production branch

**Branch protection rules ensure that:**
- All SAST security checks must pass before merging to `master`
- PRs cannot be merged without approval
- Code cannot be pushed directly to `master`
- The codebase maintains a high security baseline

## Quick Setup (GitHub UI)

### 1. Navigate to Settings

1. Go to your GitHub repository
2. Click **Settings** (top navigation)
3. In the left sidebar, click **Branches** (under "Code and automation")

### 2. Add Branch Protection Rule

1. Click **Add rule** (or **Add branch protection rule**)
2. In **Branch name pattern**, enter: `master`

### 3. Configure Protection Rules

Enable the following settings:

#### Required Status Checks

✅ **Require status checks to pass before merging**

Click **Add** and select these checks (from `.github/workflows/sast.yml`):

- ✅ `Gitleaks`
- ✅ `Semgrep (Fast)`
- ✅ `SonarQube Analysis`
- ✅ `ESLint Security`
- ✅ `Frontend Dependency Audit`
- ✅ `Trivy Security Scan`
- ✅ `Checkov IaC Scan`

✅ **Require branches to be up to date before merging** (recommended)
- Ensures PRs are tested against the latest code
- May require rebasing/merging develop before merge

#### Code Review Requirements

✅ **Require a pull request before merging**

✅ **Require approvals**: Set to `1` (or more for teams)

✅ **Dismiss stale pull request approvals when new commits are pushed**
- Re-review required if code changes after approval

☐ **Require review from Code Owners** (optional)
- Only if you have a CODEOWNERS file

✅ **Require approval of the most recent reviewable push**
- Prevents old approvals from being used

#### Additional Protections

✅ **Require conversation resolution before merging**
- All PR comments must be resolved

✅ **Require signed commits** (optional but recommended)
- Ensures commit authenticity
- Requires developers to configure GPG signing

☐ **Require linear history** (optional)
- Prevents merge commits, requires rebase
- Choose based on team preference

✅ **Do not allow bypassing the above settings**
- Even admins must follow the rules
- Recommended for production environments

#### Push Restrictions

✅ **Restrict who can push to matching branches**
- Add specific users/teams who can push (if needed)
- Or leave empty to prevent all direct pushes

### 4. Save Changes

Click **Create** (or **Save changes**)

## Important: Populate Status Checks First

**Before you can select status checks**, they must run at least once. If the dropdown is empty:

📄 **See: [docs/populate-status-checks.md](populate-status-checks.md)**

Quick solution:
1. Commit workflow changes
2. Go to **Actions** tab → **SAST - PR Checks** → **Run workflow**
3. Wait for it to complete
4. Return to branch protection settings - checks will now appear

## Verification

After setup, test that protection works:

1. **Create a test PR** from `develop` to `master`
2. **Verify status checks** appear at the bottom of the PR
3. **Attempt to merge** - should be blocked until all checks pass
4. **Wait for checks to complete**
5. **Merge** should be enabled once all checks pass

## Status Check Names Reference

These are the exact job names from `.github/workflows/sast.yml` that must pass:

| GitHub Status Check Name | Tool | Purpose |
| --- | --- | --- |
| `Gitleaks` | Gitleaks | Secret detection |
| `Semgrep (Fast)` | Semgrep | Fast source code security patterns |
| `SonarQube Analysis` | SonarQube | Code quality & security |
| `ESLint Security` | ESLint | Frontend security anti-patterns |
| `Frontend Dependency Audit` | npm audit | Frontend CVEs |
| `Trivy Security Scan` | Trivy | Dependencies, secrets, misconfigs |
| `Checkov IaC Scan` | Checkov | Infrastructure as Code |

**Note**: The status check names must match exactly. GitHub will show available checks after the first PR run.

## Advanced Configuration via API

For infrastructure-as-code or automation, use GitHub API:

```bash
# Set branch protection via API
curl -X PUT \
  -H "Authorization: token YOUR_GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/OWNER/REPO/branches/master/protection \
  -d @branch-protection.json
```

**branch-protection.json**:
```json
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "Gitleaks",
      "Semgrep (Fast)",
      "SonarQube Analysis",
      "ESLint Security",
      "Frontend Dependency Audit",
      "Trivy Security Scan",
      "Checkov IaC Scan"
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismissal_restrictions": {},
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1,
    "require_last_push_approval": true
  },
  "restrictions": null,
  "required_linear_history": false,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_conversation_resolution": true
}
```

Replace:
- `YOUR_GITHUB_TOKEN` with a personal access token (repo scope)
- `OWNER` with your GitHub username/organization
- `REPO` with your repository name

## Terraform Configuration

For GitOps/IaC approach, use Terraform GitHub provider:

```hcl
resource "github_branch_protection" "master" {
  repository_id = "your-repo-name"
  pattern       = "master"

  required_status_checks {
    strict = true
    contexts = [
      "Gitleaks",
      "Semgrep (Fast)",
      "SonarQube Analysis",
      "ESLint Security",
      "Frontend Dependency Audit",
      "Trivy Security Scan",
      "Checkov IaC Scan"
    ]
  }

  required_pull_request_reviews {
    dismiss_stale_reviews           = true
    require_code_owner_reviews      = false
    required_approving_review_count = 1
    require_last_push_approval      = true
  }

  enforce_admins                  = true
  require_conversation_resolution = true
  require_signed_commits          = false
  required_linear_history         = false

  allows_deletions    = false
  allows_force_pushes = false
}
```

## Handling Check Failures

### What Happens When a Check Fails?

When any required status check fails:
1. ❌ **Merge button is disabled** in the PR
2. **Red X** appears next to the failing check
3. **Details link** shows the failure reason
4. PR **cannot be merged** until fixed

### How to Fix and Retry

1. **View the failure**: Click "Details" next to the failed check
2. **Fix the issue**: Address the security finding in your code
3. **Push the fix**: Commit and push to the PR branch
4. **Checks re-run**: GitHub automatically re-runs all checks
5. **Merge when green**: Once all checks pass, merge is enabled

### Emergency Bypass (Not Recommended)

If you absolutely need to bypass protection (production hotfix):

1. Go to **Settings → Branches**
2. Edit the protection rule
3. Temporarily **uncheck "Do not allow bypassing the above settings"**
4. **Merge the PR**
5. **Re-enable** the setting immediately after
6. **Create a follow-up PR** to fix the skipped issues

**Warning**: Only use in true emergencies. Document the bypass in your incident log.

## Common Issues

### Check Doesn't Appear in Required Status List

**Problem**: You configured a check but it doesn't show in the dropdown

**Solution**:
1. Create a test PR first
2. Wait for the workflow to run at least once
3. GitHub learns the check names from the first run
4. Return to Settings → Branches and add the check

### "Required status check 'X' is expected"

**Problem**: GitHub expects a check but it hasn't run yet

**Causes**:
- Workflow file was changed after setting protection
- Check name doesn't match exactly
- Workflow has a conditional that skipped the job

**Solution**:
- Verify the job name in `.github/workflows/sast.yml` matches exactly
- Ensure workflows run on PRs: `on: pull_request:`
- Check workflow logs for skipped jobs

### Checks Pass but Merge Still Blocked

**Possible causes**:
- ❌ **Approvals required** but no one has approved
- ❌ **Conversations unresolved** - check PR comments
- ❌ **Branch not up to date** - merge/rebase develop
- ❌ **Direct push restrictions** - you may not have permission

## Best Practices

### For Solo Developers

- Require at least 1 approval (use a second GitHub account or ask a colleague)
- Use signed commits for authenticity
- Don't bypass protections - it defeats the purpose

### For Teams

- Require 2 approvals for critical branches
- Enable "Require review from Code Owners"
- Create a CODEOWNERS file to auto-assign reviewers
- Use "Require conversation resolution" to ensure all feedback is addressed

### For Organizations

- Enforce admins - no exceptions
- Require signed commits
- Use branch protection as part of compliance (SOC 2, ISO 27001)
- Audit bypass events regularly
- Document all protection changes

## Protecting Additional Branches

You may also want to protect:

**Development branch:**
```
Pattern: develop
Lighter protections than master:
- Require status checks (optional)
- Allow force pushes for rebasing
- Fewer required approvals
```

**Release branches:**
```
Pattern: release/*
- Require approvals
- Require status checks
- Allow force pushes for rebasing (optional)
```

**Hotfix branches:**
```
Pattern: hotfix/*
- Require status checks
- Expedited review process
```

## Integration with CODEOWNERS

Create a `.github/CODEOWNERS` file:

```
# Global owners (optional)
* @your-username

# Security-sensitive files
/.github/workflows/ @security-team
/sonar-project.properties @security-team
/docs/sast-strategy.md @security-team

# Backend
/src/backend/ @backend-team

# Frontend
/src/frontend/ @frontend-team
```

Then enable "Require review from Code Owners" in branch protection.

## Monitoring and Compliance

### Audit Branch Protection Changes

GitHub logs all protection rule changes:

1. Go to **Settings → Audit log**
2. Filter by `action:protected_branch.*`
3. Review changes regularly

### Enforcement Reports

Track merge compliance:

- **Merged PRs without approval**: Should be 0
- **Bypassed checks**: Document in incident log
- **Average time to pass checks**: Monitor for bottlenecks

### Alerts

Set up notifications for protection changes:

1. **Organization admins**: Receive alerts for protection rule changes
2. **Slack/Discord webhooks**: Notify team of bypasses
3. **SIEM integration**: Log protection events for audit

## Resources

- [GitHub Branch Protection Docs](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [GitHub API - Branch Protection](https://docs.github.com/en/rest/branches/branch-protection)
- [GitHub CODEOWNERS](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners)
