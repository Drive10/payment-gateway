# GitHub Repository Settings

> Configure these settings in your GitHub repository for maximum security.

## Branch Protection Rules

### Main Branch Protection
Navigate to: **Settings → Branches → Add rule**

**Branch name pattern**: `main`

#### Protect matching branches
- [x] Require a pull request before merging
  - [x] Require approvals: **2**
  - [x] Dismiss stale pull request approvals when new commits are pushed
  - [x] Require review from Code Owners
- [x] Require status checks to pass before merging
  - [x] Require branches to be up to date before merging
  - **Required status checks**:
    - `CodeQL Analysis (java)`
    - `CodeQL Analysis (javascript)`
    - `Dependency Review`
    - `Maven Dependency Scan`
    - `NPM Audit (dashboard)`
    - `NPM Audit (frontend)`
    - `Secret Scanning`
    - `Docker Security Scan`
- [x] Require conversation resolution before merging
- [x] Include administrators
- [x] Restrict who can push to matching branches
  - Add specific users/teams (not "Everyone")
- [ ] Allow force pushes: **false**
- [ ] Allow deletions: **false**
- [x] Require signed commits
- [x] Require linear history

## Repository Settings

### Code Security and Analysis
Navigate to: **Settings → Code security and analysis**

- [x] **Secret scanning**: Push protection **ON**
- [x] **Secret scanning**: Alert notifications **ON**
- [x] **Dependabot alerts**: **ON**
- [x] **Dependabot security updates**: **ON**
- [x] **Dependabot version updates**: **ON** (configured in `.github/dependabot.yml`)
- [x] **Code scanning**: **ON** (configured in `.github/workflows/codeql.yml`)

### Actions Permissions
Navigate to: **Settings → Actions → General**

- [x] **Allow all actions and reusable workflows**
- [x] **Allow actions created by GitHub**
- [x] **Allow actions by Marketplace verified creators**
- [ ] **Allow specified actions** (recommended for high-security repos)
  - Only allow actions from trusted publishers

### Fork Settings
Navigate to: **Settings → General → Features**

- [x] **Allow forking**: **ON** (encourages community contributions)
- [ ] **Allow forking**: **OFF** (for private/internal repos)

### Pull Request Settings
Navigate to: **Settings → General → Pull Requests**

- [x] **Always suggest updating pull request branches**
- [x] **Allow auto-merge** (after all checks pass)
- [ ] **Allow rebase merging**: **OFF** (prefer squash merge for clean history)
- [x] **Allow squash merging**: **ON**
- [ ] **Allow merge commits**: **OFF** (prefer squash for linear history)
- [x] **Default commit message**: Pull request title + description

### Issues Settings
Navigate to: **Settings → General → Features**

- [x] **Issues**: **ON**
- [x] **Projects**: **ON**
- [ ] **Wikis**: **OFF** (use docs/ instead)

### Pages Settings
Navigate to: **Settings → Pages**

- [x] **GitHub Pages**: **OFF** (no public documentation hosting)

## Environment Protection Rules

### Production Environment
Navigate to: **Settings → Environments → New environment: `production`**

- [x] **Required reviewers**: Add 2 reviewers
- [x] **Wait timer**: 5 minutes
- [x] **Deployment branches**: Only `main` branch

### Staging Environment
Navigate to: **Settings → Environments → New environment: `staging`**

- [x] **Required reviewers**: Add 1 reviewer
- [x] **Deployment branches**: Only `main` branch

## Webhook Security

Navigate to: **Settings → Webhooks**

- [x] **SSL verification**: **ON**
- [x] **Secret**: Set a strong webhook secret
- [x] **Content type**: `application/json`
- [x] **Active**: Only for verified endpoints

## Security Advisories

Navigate to: **Security → Advisories**

- [x] **Private vulnerability reporting**: **ON**
- [x] **Security policy**: Link to `SECURITY.md`

## Actions Variables

Navigate to: **Settings → Actions → Variables**

### Repository Variables
| Variable | Value | Description |
|----------|-------|-------------|
| `REGISTRY` | `ghcr.io` | Container registry URL |
| `IMAGE_PREFIX` | `payment-gateway` | Docker image prefix |

### Repository Secrets
| Secret | Description |
|--------|-------------|
| `SONAR_TOKEN` | SonarQube authentication token |
| `SEMGREP_APP_TOKEN` | Semgrep SAST scanning token |
| `DEPLOY_KEY` | SSH key for deployment |
| `AWS_ACCESS_KEY_ID` | AWS credentials (if using AWS) |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key |
| `VAULT_TOKEN` | HashiCorp Vault token |

## Recommended GitHub Apps

| App | Purpose |
|-----|---------|
| **Dependabot** | Automated dependency updates |
| **CodeQL** | Static analysis |
| **Trivy** | Container scanning |
| **Snyk** | Vulnerability scanning |
| **SonarCloud** | Code quality |
| **Renovate** | Alternative to Dependabot |

## Checklist for New Contributors

1. [ ] Sign CLA (Contributor License Agreement)
2. [ ] Read CONTRIBUTING.md
3. [ ] Review SECURITY.md
4. [ ] Fork the repository
5. [ ] Create feature branch from `main`
6. [ ] Make changes with tests
7. [ ] Submit pull request
8. [ ] Address review comments
9. [ ] Squash and merge after approval
