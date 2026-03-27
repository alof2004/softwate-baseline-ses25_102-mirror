# How to Populate Status Checks for Branch Protection

GitHub only shows status checks in the branch protection dropdown **after they've run at least once**. Here are three ways to trigger the initial workflow run.

## Option 1: Manual Workflow Trigger (Fastest - 10 minutes)

I've added `workflow_dispatch` to the SAST workflow, allowing manual triggering.

### Steps:

1. **Commit and push the workflow changes:**

```bash
cd softwate-baseline-ses25_102
git add .github/workflows/sast.yml
git add docs/
git add CLAUDE.md
git add sonar-project.properties
git commit -m "Add SonarQube and update for master branch protection"
git push origin develop
```

2. **Manually trigger the workflow on GitHub:**
   - Go to your repository on GitHub
   - Click **Actions** tab
   - In the left sidebar, click **SAST - PR Checks**
   - Click **Run workflow** button (top right)
   - Select branch: `develop` (or `master`)
   - Click **Run workflow**

3. **Wait for the workflow to complete** (5-10 minutes)
   - All 7 jobs will run
   - Watch the progress in the Actions tab

4. **Set up branch protection:**
   - Go to **Settings → Branches**
   - Add rule for `master`
   - Enable "Require status checks to pass before merging"
   - **The checks will now appear in the dropdown!**
   - Select all 7 checks:
     - Gitleaks
     - Semgrep (Fast)
     - SonarQube Analysis
     - ESLint Security
     - Frontend Dependency Audit
     - Trivy Security Scan
     - Checkov IaC Scan

## Option 2: Create a Test PR (Most Realistic - 10 minutes)

This simulates the actual PR workflow.

### Steps:

1. **Ensure your changes are committed:**

```bash
cd softwate-baseline-ses25_102
git status
git add .
git commit -m "Add SonarQube and update for master branch protection"
git push origin develop
```

2. **Create a PR from develop to master:**
   - Go to GitHub → **Pull Requests** → **New Pull Request**
   - Base: `master`
   - Compare: `develop`
   - Title: "Test: Populate branch protection checks"
   - Click **Create Pull Request**

3. **Wait for checks to run:**
   - The SAST workflow will trigger automatically
   - All 7 checks will appear at the bottom of the PR
   - Wait for them to complete (or you can cancel after they start)

4. **Set up branch protection:**
   - The checks are now registered with GitHub
   - Go to **Settings → Branches** → Add rule
   - The checks will appear in the dropdown

5. **Close the test PR** (optional):
   - You can close/delete it if you don't want to merge yet

## Option 3: Empty Commit (Lightweight - 10 minutes)

Quick way without making real changes.

### Steps:

1. **Create an empty commit:**

```bash
cd softwate-baseline-ses25_102
git checkout develop
git commit --allow-empty -m "Trigger workflow for branch protection setup"
git push origin develop
```

2. **Create a PR:**
   - Follow the same PR creation steps as Option 2
   - This PR will have no file changes, just triggers the workflow

3. **Wait and configure:**
   - Let the workflow run
   - Set up branch protection
   - Close the PR

## Verification

After running any of the above options:

1. **Go to Settings → Branches**
2. **Add rule** for `master`
3. **Enable** "Require status checks to pass before merging"
4. **Click the search box** under "Status checks that are required"
5. **You should see all 7 checks:**
   - ✅ Gitleaks
   - ✅ Semgrep (Fast)
   - ✅ SonarQube Analysis
   - ✅ ESLint Security
   - ✅ Frontend Dependency Audit
   - ✅ Trivy Security Scan
   - ✅ Checkov IaC Scan

If you don't see them, check:
- Workflow file is pushed to the repository
- Workflow has completed (not failed or still running)
- You're on the correct repository

## Troubleshooting

### "No status checks found"

**Cause:** Workflow hasn't run yet or failed

**Solution:**
- Go to **Actions** tab
- Check if "SAST - PR Checks" workflow exists
- Check if it ran successfully
- Look for any errors in the workflow logs

### Checks appear but can't be selected

**Cause:** Checks failed or are still running

**Solution:**
- Wait for checks to complete
- Refresh the branch protection settings page
- The checks must complete at least once (pass or fail)

### Workflow doesn't trigger

**Cause:** Workflow file syntax error or not on correct branch

**Solution:**
- Check `.github/workflows/sast.yml` exists in your repository
- Verify YAML syntax is correct
- Ensure the file is pushed to GitHub
- Check the Actions tab for any errors

## After Setup

Once branch protection is configured, **you can remove `workflow_dispatch`** from the workflow if desired:

```yaml
# Remove this line from .github/workflows/sast.yml
workflow_dispatch:  # Allows manual trigger from GitHub UI
```

This prevents accidental manual runs, but it's harmless to leave it there for future testing.

## Next Steps

After branch protection is set up:

1. ✅ Create PRs from `develop` to `master`
2. ✅ Checks run automatically on every PR
3. ✅ Merge is blocked until all checks pass
4. ✅ Your master branch is protected!
