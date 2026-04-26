# Branch Protection Rules Setup

## Overview
This document describes how to configure Branch Protection Rules for the `main` and `develop` branches to ensure code quality and prevent direct pushes without passing CI checks.

## GitHub Branch Protection Configuration

### Step 1: Navigate to Repository Settings
1. Go to your GitHub repository
2. Click on **Settings** → **Branches**
3. Click **Add rule** or edit existing rule

### Step 2: Configure Protection Rules for `main` branch

#### Branch name pattern
```
main
```

#### Protection Settings

**Require a pull request before merging**
- ✅ Enable this option
- Require approvals: **1** (minimum)
- ✅ Dismiss stale pull request approvals when new commits are pushed
- ✅ Require review from Code Owners (if CODEOWNERS file exists)

**Require status checks to pass before merging**
- ✅ Enable this option
- ✅ Require branches to be up to date before merging
- Required status checks:
  - `Java Backend Tests`
  - `C++ Availability Engine Tests`
  - `Security Vulnerability Scan`
  - `Quality Gate`

**Require conversation resolution before merging**
- ✅ Enable this option

**Require signed commits**
- ⬜ Optional (recommended for production)

**Require linear history**
- ✅ Enable this option (prevents merge commits)

**Do not allow bypassing the above settings**
- ✅ Enable this option

**Restrict who can push to matching branches**
- ✅ Enable this option
- Add: Repository administrators only

**Allow force pushes**
- ⬜ Disabled (never allow force push to main)

**Allow deletions**
- ⬜ Disabled (never allow branch deletion)

### Step 3: Configure Protection Rules for `develop` branch

#### Branch name pattern
```
develop
```

#### Protection Settings
Same as `main` branch, but with slightly relaxed rules:

**Require a pull request before merging**
- ✅ Enable this option
- Require approvals: **1**
- ✅ Dismiss stale pull request approvals when new commits are pushed

**Require status checks to pass before merging**
- ✅ Enable this option
- ✅ Require branches to be up to date before merging
- Required status checks:
  - `Java Backend Tests`
  - `C++ Availability Engine Tests`
  - `Security Vulnerability Scan`

**Allow force pushes**
- ⬜ Disabled

### Step 4: Verify Configuration

After setting up the rules, verify by:

1. Creating a test branch from `develop`
2. Making a small change
3. Opening a Pull Request
4. Observing that:
   - CI pipeline runs automatically
   - Status checks appear in the PR
   - Merge button is disabled until all checks pass
   - At least 1 approval is required

## CI Status Checks

The following status checks will be automatically run on every PR:

### 1. Java Backend Tests
- Compiles Java code
- Runs Checkstyle linting
- Executes unit tests
- Executes integration tests with Testcontainers
- Generates JaCoCo coverage report
- Runs SonarQube analysis (if configured)

### 2. C++ Availability Engine Tests
- Compiles C++ code with CMake
- Runs clang-tidy static analysis
- Executes Google Test unit tests
- Tests scheduling algorithm correctness

### 3. Security Vulnerability Scan
- Trivy filesystem scan for Java dependencies
- Trivy filesystem scan for C++ dependencies
- Snyk security scan (if token configured)
- Uploads results to GitHub Security tab

### 4. Quality Gate
- Aggregates results from all jobs
- Fails if any critical job fails
- Provides summary of all checks

## Required Secrets

Configure these secrets in **Settings** → **Secrets and variables** → **Actions**:

### Optional (for enhanced features)
- `SONAR_TOKEN`: SonarQube authentication token
- `SONAR_HOST_URL`: SonarQube server URL (e.g., https://sonarcloud.io)
- `SNYK_TOKEN`: Snyk API token for security scanning

### How to add secrets
1. Go to repository **Settings**
2. Click **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add name and value
5. Click **Add secret**

## Workflow Triggers

The CI pipeline runs on:
- **Push** to `main` or `develop` branches
- **Pull Request** targeting `main` or `develop` branches

## Quality Gates

### Coverage Threshold
- Minimum code coverage: **80%** (configured in JaCoCo)
- Coverage report available as artifact after each run

### Test Requirements
- All unit tests must pass
- All integration tests must pass
- No critical security vulnerabilities

### Build Requirements
- Java code must compile without errors
- C++ code must compile without errors
- Checkstyle and clang-tidy warnings are logged but don't fail the build

## Troubleshooting

### Status checks not appearing
- Ensure the workflow file is in `.github/workflows/ci.yml`
- Check that the workflow has run at least once on the target branch
- Verify workflow syntax with `yamllint`

### Tests failing in CI but passing locally
- Check environment variables (database URLs, credentials)
- Verify service containers are healthy (PostgreSQL, Redis)
- Review logs in GitHub Actions tab

### Merge button still disabled after checks pass
- Ensure at least 1 approval is provided
- Check that all conversations are resolved
- Verify branch is up to date with base branch

## Best Practices

1. **Never commit directly to `main` or `develop`**
   - Always create feature branches
   - Use naming convention: `feature/`, `fix/`, `hotfix/`

2. **Keep PRs small and focused**
   - Easier to review
   - Faster CI execution
   - Reduced merge conflicts

3. **Write meaningful commit messages**
   - Follow conventional commits format
   - Example: `feat: add room availability caching`

4. **Monitor CI performance**
   - Review cache hit rates
   - Optimize slow tests
   - Parallelize where possible

5. **Address security findings promptly**
   - Review Trivy/Snyk reports
   - Update vulnerable dependencies
   - Don't ignore security warnings

## GitLab Alternative

If using GitLab instead of GitHub, configure protected branches in:
**Settings** → **Repository** → **Protected branches**

Similar rules apply with GitLab CI/CD pipelines defined in `.gitlab-ci.yml`.
