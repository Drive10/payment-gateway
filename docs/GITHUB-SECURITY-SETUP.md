# GitHub Repository Settings

> Configure these settings in your GitHub repository for maximum security.

## Branch Protection Rules

### Main Branch Protection
Navigate to: **Settings → Branches → Add rule**

**Branch name pattern**: `main`

#### Protect matching branches
- [x] Require a pull request before merging
  - [x] Require approvals: **1**
  - [x] Dismiss stale pull request approvals when new commits are pushed
- [x] Require status checks to pass before merging
  - [x] Require branches to be up to date before merging
  - **Required status checks**:
    - `build`
    - `test`
    - `security-scan`
- [x] Require conversation resolution before merging
- [ ] Allow force pushes: **false**
- [ ] Allow deletions: **false**
- [x] Require signed commits

## Repository Settings

### Code Security and Analysis
Navigate to: **Settings → Code security and analysis**

- [x] **Secret scanning**: Push protection **ON**
- [x] **Secret scanning**: Alert notifications **ON**
- [x] **Dependabot alerts**: **ON**
- [x] **Dependabot security updates**: **ON**
- [x] **Dependabot version updates**: **ON** (configured in `.github/dependabot.yml`)

### Actions Permissions
Navigate to: **Settings → Actions → General**

- [x] **Allow all actions and reusable workflows**
- [x] **Allow actions created by GitHub**

### Fork Settings
Navigate to: **Settings → General → Features**

- [x] **Allow forking**: **ON** (encourages community contributions)

### Pull Request Settings
Navigate to: **Settings → General → Pull Requests**

- [x] **Always suggest updating pull request branches**
- [x] **Allow squash merging**: **ON**
- [ ] **Allow merge commits**: **OFF** (prefer squash for clean history)
- [x] **Default commit message**: Pull request title + description

### Issues Settings
Navigate to: **Settings → General → Features**

- [x] **Issues**: **ON**
- [ ] **Wikis**: **OFF** (use docs/ instead)

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

### Repository Secrets
| Secret | Description |
|--------|-------------|
| `GHCR_TOKEN` | GitHub Container Registry authentication |

## Recommended GitHub Apps

| App | Purpose |
|-----|---------|
| **Dependabot** | Automated dependency updates |
| **CodeQL** | Static analysis |
| **Trivy** | Container scanning |

## Checklist for New Contributors

1. [ ] Read CONTRIBUTING.md
2. [ ] Review SECURITY.md
3. [ ] Fork the repository
4. [ ] Create feature branch from `main`
5. [ ] Make changes with tests
6. [ ] Submit pull request
7. [ ] Address review comments
8. [ ] Squash and merge after approval
