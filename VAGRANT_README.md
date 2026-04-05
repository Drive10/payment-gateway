# Payment Gateway - Development VM (Vagrant)

A Vagrant VM configuration for running the payment gateway project with all required infrastructure (Docker, PostgreSQL, Kafka, Redis, Vault).

## Prerequisites

- [VirtualBox](https://www.virtualbox.org/wiki/Downloads) (version 7.0+)
- [Vagrant](https://www.vagrantup.com/downloads) (version 2.4+)

## Quick Start

```bash
# Clone the project or navigate to project directory
cd payment-gateway

# Start the VM
vagrant up

# SSH into the VM
vagrant ssh

# Inside VM: Start infrastructure services
cd /workspace
docker compose -f docker-compose.dev.yml up -d

# Check services status
docker ps

# Run seed script (creates demo data)
./scripts/seed.sh
```

## Usage

### Basic Commands

| Command | Description |
|---------|-------------|
| `vagrant up` | Create and start the VM |
| `vagrant ssh` | SSH into the VM |
| `vagrant halt` | Stop the VM |
| `vagrant destroy` | Destroy the VM |
| `vagrant status` | Show VM status |
| `vagrant reload` | Restart the VM |

### Custom Git Workspace

Override the synced folder path:

```bash
vagrant up -- --gitlocal-dir "C:/path/to/your/workspace"
```

## VM Configuration

| Setting | Value |
|---------|-------|
| OS | Rocky Linux 9 |
| Memory | 8 GB |
| CPUs | 4 |
| IP | 192.168.33.10 |
| Docker | 24.0.7 |
| Compose | 2.23.3 |

## Services

Once running, access these services:

| Service | URL |
|---------|-----|
| Frontend (Checkout) | http://192.168.33.10:3000 |
| Dashboard (Admin) | http://192.168.33.10:3001 |
| API Gateway | http://192.168.33.10:8080 |
| Vault | http://192.168.33.10:8200 |
| PostgreSQL | localhost:5433 |
| Kafka | localhost:9092 |
| Redis | localhost:6379 |

## Demo Credentials

After running seed script:
- **Admin**: admin@payflow.com / Test@1234
- **User**: john@payflow.com / Test@1234

## Troubleshooting

### Docker not starting
```bash
vagrant ssh
sudo systemctl status docker
sudo systemctl restart docker
```

### VM not getting IP
```bash
vagrant reload
```

### Rebuild from scratch
```bash
vagrant destroy -f
vagrant up
```

## Notes

- The VM uses a synced folder to share your host workspace with the guest
- Changes to files on your host are immediately available in the VM
- Docker daemon is configured to allow insecure registry on 192.168.33.10:5000
- SELinux is set to permissive mode for development ease
