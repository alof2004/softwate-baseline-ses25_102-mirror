# SonarQube Setup Guide

This guide explains the SonarQube setup for the clinic management application.

## Overview

**The GitHub Actions workflow runs SonarQube automatically in each PR** as an ephemeral Docker service container. No external setup is required!

### How It Works

1. **Service Container**: GitHub Actions spins up a SonarQube container for each PR run
2. **Auto-Configuration**: The workflow automatically creates the project and generates a token
3. **Analysis**: Code is scanned against the ephemeral SonarQube instance
4. **Quality Gate**: PR is blocked if quality gate fails
5. **Cleanup**: Container is destroyed after the workflow completes

### Benefits

- ✅ **Zero infrastructure setup** - No servers to maintain
- ✅ **No GitHub secrets required** - Everything is self-contained
- ✅ **Fresh instance per PR** - No state pollution between runs
- ✅ **Faster setup** - Works immediately out of the box

## No Setup Required

The PR workflow (`.github/workflows/sast.yml`) includes:

```yaml
services:
  sonarqube:
    image: sonarqube:10-community
    # ... configuration
```

This automatically runs SonarQube on `http://localhost:9000` during the workflow.

## Optional: Local SonarQube for Development

If you want to run SonarQube locally for development/testing:

### Prerequisites

- Docker and Docker Compose installed
- Minimum 2GB RAM available for SonarQube

### Quick Local Setup

Create a `docker-compose.sonarqube.yml` file:

```yaml
version: "3"

services:
  sonarqube:
    image: sonarqube:10-community
    container_name: sonarqube
    depends_on:
      - sonarqube-db
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://sonarqube-db:5432/sonar
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: sonar
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_extensions:/opt/sonarqube/extensions
      - sonarqube_logs:/opt/sonarqube/logs
    ports:
      - "9000:9000"
    networks:
      - sonarnet

  sonarqube-db:
    image: postgres:16
    container_name: sonarqube-db
    environment:
      POSTGRES_USER: sonar
      POSTGRES_PASSWORD: sonar
      POSTGRES_DB: sonar
    volumes:
      - postgresql_data:/var/lib/postgresql/data
    networks:
      - sonarnet

volumes:
  sonarqube_data:
  sonarqube_extensions:
  sonarqube_logs:
  postgresql_data:

networks:
  sonarnet:
    driver: bridge
```

Start the server:

```bash
docker-compose -f docker-compose.sonarqube.yml up -d
```

Wait 2-3 minutes for SonarQube to fully start, then access it at: http://localhost:9000

### Local Configuration

1. **Login** with default credentials:
   - Username: `admin`
   - Password: `admin`

2. **Change the password** when prompted (required on first login)

3. **Create a Project**:
   - Click "Create Project" → "Manually"
   - Project key: `clinic-management-app` (must match `sonar-project.properties`)
   - Project name: `Clinic Management Application`
   - Branch: `master`
   - Click "Set Up"

4. **Generate Token**:
   - Token name: `local-dev`
   - Type: User Token
   - Expiration: No expiration
   - Click "Generate"
   - **Copy the token immediately** (you won't see it again)

### Running Local Scans

With SonarQube running locally, you can scan your code:

```bash
# Using sonar-scanner CLI
docker run --rm \
  --network host \
  -v "$PWD:/usr/src" \
  sonarsource/sonar-scanner-cli \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN_HERE

# Or use Maven (for backend only)
cd src/backend
./mvnw sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN_HERE
```

## Advanced: Persistent SonarQube Setup

The GitHub Actions ephemeral approach works great for PR checks, but if you want historical trend analysis or persistent quality metrics across PRs, you need a persistent SonarQube instance.

### Why Persistent SonarQube?

- **Historical trends**: Track code quality metrics over time
- **Project dashboard**: Centralized view of all quality metrics
- **Technical debt tracking**: Monitor accumulation and reduction
- **PR decoration**: Inline comments on GitHub PRs with findings

### Deployment Options

#### A. Cloud VM with Public IP

1. **Deploy SonarQube on a cloud provider**:
   - AWS EC2, Google Cloud Compute Engine, Azure VM, DigitalOcean Droplet, etc.
   - Minimum specs: 2 vCPUs, 4GB RAM, 20GB disk
   - Open port 9000 in security groups/firewall

2. **Configure HTTPS** (recommended):
   - Use a reverse proxy (Nginx/Apache) with Let's Encrypt SSL certificate
   - Example Nginx config:

   ```nginx
   server {
       listen 80;
       server_name sonar.yourdomain.com;
       return 301 https://$server_name$request_uri;
   }

   server {
       listen 443 ssl;
       server_name sonar.yourdomain.com;

       ssl_certificate /etc/letsencrypt/live/sonar.yourdomain.com/fullchain.pem;
       ssl_certificate_key /etc/letsencrypt/live/sonar.yourdomain.com/privkey.pem;

       location / {
           proxy_pass http://localhost:9000;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
       }
   }
   ```

3. **Configure GitHub Workflow**: Update `.github/workflows/sast.yml` to use your persistent instance:

   ```yaml
   sonarqube:
     name: SonarQube Analysis
     runs-on: ubuntu-latest
     # Remove the "services:" section
     steps:
       # ... existing steps ...

       # Remove "Wait for SonarQube" and "Configure SonarQube project" steps

       - name: Run SonarQube scan
         uses: SonarSource/sonarqube-scan-action@v3
         env:
           SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}  # https://sonar.yourdomain.com
           SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}        # Your token
   ```

4. **Add GitHub Secrets**:
   - `SONAR_HOST_URL`: `https://sonar.yourdomain.com`
   - `SONAR_TOKEN`: Token from SonarQube UI

#### B. Ngrok/Tailscale Tunnel (Quick Testing)

For quick testing without a cloud VM:

```bash
# Using ngrok
ngrok http 9000
# Copy the HTTPS URL (e.g., https://abc123.ngrok.io)
# Add to GitHub secrets as SONAR_HOST_URL

# Note: ngrok free tier URLs expire after 2 hours
```

#### C. GitHub Self-Hosted Runner

If you have a self-hosted GitHub Actions runner on the same network as SonarQube:

1. Set up a self-hosted runner (GitHub docs)
2. Modify the workflow to run on: `self-hosted` instead of `ubuntu-latest`
3. Use `http://localhost:9000` or internal network address
4. Keep the ephemeral approach or use secrets for a persistent instance

## SonarQube Configuration

### Quality Gate

SonarQube's default "Sonar way" quality gate is reasonable, but you can customize:

1. **Go to Quality Gates** → "Sonar way" → Copy
2. **Customize thresholds**:
   - Coverage on New Code: >80%
   - Duplicated Lines on New Code: <3%
   - Security Hotspots Reviewed: 100%
   - Reliability Rating on New Code: A
   - Security Rating on New Code: A
   - Maintainability Rating on New Code: A

3. **Set as default** for your project

### Quality Profiles

SonarQube comes with default quality profiles for Java and JavaScript. You can customize:

1. **Quality Profiles** → Java → Copy "Sonar way"
2. Activate additional security rules:
   - Search for "security" tag
   - Enable rules relevant to your project
3. **Set as default** for your project

## Troubleshooting

### GitHub Actions: SonarQube Service Won't Start

**Error**: Container health check failing

**Solutions**:
- Check GitHub Actions logs for specific error messages
- The workflow waits up to 5 minutes for SonarQube to start
- If consistently failing, the SonarQube image may be temporarily unavailable

### GitHub Actions: "Failed to generate token"

**Error**: Token extraction fails in "Configure SonarQube project" step

**Solutions**:
- SonarQube may not be fully initialized
- Try increasing the `sleep 10` to `sleep 20` in the workflow
- Check if the API endpoint has changed in newer SonarQube versions

### Local: SonarQube Container Won't Start

```bash
# Check logs
docker-compose -f docker-compose.sonarqube.yml logs sonarqube

# Common issues:
# - Insufficient memory: Increase Docker memory limit to 2GB+
# - Port 9000 in use: Change port mapping or stop conflicting service
# - Elasticsearch bootstrap checks: Add SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
```

### Persistent Setup: GitHub Actions Can't Reach SonarQube

**Error**: "java.net.ConnectException: Connection refused"

**Causes**:
- You modified the workflow to use a persistent instance but `SONAR_HOST_URL` is not accessible from GitHub Actions
- Firewall blocking port 9000/443
- SonarQube server is down

**Solutions**:
- Verify SonarQube is running: `curl https://sonar.yourdomain.com/api/system/status`
- Ensure URL is publicly accessible from the internet
- Check firewall rules and security groups

### Quality Gate Fails

**Error**: "Quality gate failed: ..."

**Solutions**:
- Review the SonarQube report (link provided in GitHub Actions log)
- Fix the issues identified
- Adjust quality gate thresholds if needed (but don't weaken security rules)

### Authentication Errors

**Error**: "Unauthorized" or "403 Forbidden"

**Causes**:
- Invalid or expired SONAR_TOKEN
- Token doesn't have project analysis permissions

**Solutions**:
- Regenerate token in SonarQube
- Update GitHub secret
- Ensure token has "Execute Analysis" permission

## Advanced: Pull Request Decoration

To see SonarQube findings directly in GitHub PRs:

1. **Install GitHub App** in SonarQube:
   - Administration → Configuration → GitHub
   - Follow the wizard to create a GitHub App
   - Install the app on your repository

2. **Configure Project**:
   - Project Settings → Pull Requests → GitHub
   - Select your GitHub App configuration

3. SonarQube will now comment on PRs with quality gate status

## Maintenance

### Backup SonarQube Data

```bash
# Backup volumes
docker run --rm \
  -v sonarqube_data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/sonarqube-data-$(date +%Y%m%d).tar.gz /data

# Backup database
docker exec sonarqube-db pg_dump -U sonar sonar > backups/sonar-db-$(date +%Y%m%d).sql
```

### Update SonarQube

```bash
# Stop containers
docker-compose -f docker-compose.sonarqube.yml down

# Update image version in docker-compose.sonarqube.yml
# sonarqube:10-community → sonarqube:11-community

# Start with new version
docker-compose -f docker-compose.sonarqube.yml up -d

# SonarQube will automatically migrate the database
```

## Resources

- [SonarQube Documentation](https://docs.sonarqube.org/latest/)
- [SonarQube Docker Image](https://hub.docker.com/_/sonarqube)
- [GitHub Actions Integration](https://docs.sonarqube.org/latest/devops-platform-integration/github-integration/)
- [Quality Gates](https://docs.sonarqube.org/latest/user-guide/quality-gates/)
- [Security Rules](https://rules.sonarsource.com/java/type/Security%20Hotspot/)
