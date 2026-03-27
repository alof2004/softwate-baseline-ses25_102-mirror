# Bootstrap Workflow on Master Branch

This guide explains how to get the SAST workflow onto the `master` branch for the first time.

## The Problem

GitHub runs workflows from **the target branch** of a PR, not the source branch.

**This means:**
- PR from `develop` → `master` uses workflows on `master` branch
- If workflows don't exist on `master`, checks won't run
- But you want checks to run BEFORE merging to `master`
- **Chicken and egg problem!**

## Solution: Bootstrap Process

I've temporarily configured the workflow to run on **both** `develop` and `master` branches, allowing you to bootstrap the setup.

### Step-by-Step Bootstrap

**1. Commit and push workflow to develop:**

```bash
cd softwate-baseline-ses25_102

# Check what's changed
git status

# Add all changes
git add .github/workflows/
git add docs/
git add sonar-project.properties
git add CLAUDE.md

# Commit
git commit -m "Add SonarQube and SAST workflows for master branch protection"

# Push to develop
git push origin develop
```

**2. Trigger initial workflow run on develop:**

Since the workflow now triggers on `develop` branch:

Option A - **Automatic** (if you just pushed):
- The workflow runs automatically on push to develop
- Go to GitHub → Actions tab to watch it run

Option B - **Manual trigger**:
- Go to GitHub → Actions → SAST - PR Checks → Run workflow
- Select branch: `develop`
- Click Run workflow

**3. Wait for workflow to complete (5-10 minutes)**

This populates the status check names in GitHub's database.

**4. Create PR from develop to master:**

- Go to GitHub → Pull Requests → New Pull Request
- Base: `master`
- Compare: `develop`
- Title: "Add SAST security workflows and SonarQube"
- Click Create Pull Request

**5. The workflow will run on this PR!**

Because we added `develop` as a trigger branch temporarily, the workflow will run even though it's not on `master` yet.

**6. Wait for all checks to pass (or review failures)**

All 7 status checks should run:
- Gitleaks ✓
- Semgrep (Fast) ✓
- SonarQube Analysis ✓
- ESLint Security ✓
- Frontend Dependency Audit ✓
- Trivy Security Scan ✓
- Checkov IaC Scan ✓

**7. Merge the PR to master**

Since `master` is not yet protected, you can merge freely. This is okay - it's a one-time bootstrap!

**8. NOW set up branch protection:**

- Go to Settings → Branches → Add rule
- Pattern: `master`
- Enable "Require status checks to pass before merging"
- **Select all 7 checks** (they'll appear in the dropdown now!)
- Enable other protections (approvals, conversation resolution, etc.)
- Save

**9. Clean up the temporary triggers:**

Remove the `develop` branch triggers from the workflow:

```bash
git checkout develop

# Edit .github/workflows/sast.yml
# Remove these two lines:
#   - develop  # Temporary: for bootstrapping workflows
```

Or let me do it for you after you merge to master.

**10. Test it works:**

Create a test PR from `develop` to `master` and verify:
- ✓ All checks run automatically
- ✓ Merge is blocked until checks pass
- ✓ Branch protection is working!

## Alternative: Direct Push (Simpler)

If `master` is not yet protected and you have push access:

```bash
# Just merge develop into master directly
git checkout master
git pull origin master
git merge develop
git push origin master

# Then set up branch protection
```

This is simpler but bypasses the PR process for this one-time setup.

## After Bootstrap

Once the workflow is on `master` and protection is set up:

✅ **All future PRs** from `develop` → `master` will:
- Run all 7 SAST checks automatically
- Block merge until checks pass
- Require approval before merging

✅ **You can remove** the temporary `develop` triggers from the workflow

✅ **Master is now protected** with comprehensive security gates

## Verification Checklist

After completing the bootstrap:

- [ ] Workflow file exists on `master` branch
- [ ] Branch protection rule created for `master`
- [ ] All 7 status checks selected as required
- [ ] Test PR created and checks run automatically
- [ ] Merge button disabled until checks pass
- [ ] Temporary `develop` triggers removed (optional cleanup)

## Troubleshooting

### "Checks don't appear in branch protection dropdown"

**Solution:** The workflow needs to run at least once. Create a test PR or use manual trigger.

### "Workflow doesn't run on PR"

**Cause:** Workflow file not on the target branch (`master`)

**Solution:** Complete the bootstrap process above to get it onto `master`.

### "Can't push to master - it's already protected"

**Cause:** Someone already set up branch protection

**Solution:**
- Temporarily disable protection
- OR create a PR and have an admin merge it
- OR have an admin add you to bypass list temporarily

## Why This Works

The temporary dual-branch trigger allows:

1. **develop branch**: Runs workflow to populate check names
2. **PR to master**: Uses workflow from `develop` (since we added it as a trigger)
3. **Merge to master**: Gets the workflow onto `master`
4. **Future PRs**: Use workflow from `master` (normal operation)
5. **Cleanup**: Remove `develop` triggers, keeping only `master`

This is a standard bootstrap pattern for new security policies!
