# -*- mode: ruby -*-
# vi: set ft=ruby :

require 'yaml'

# Configuration
VM_NAME = "payment-gateway-dev"
VM_MEMORY = 8192
VM_CPUS = 4
VM_IP = "192.168.33.10"
DOCKER_VERSION = "27.3.1"
COMPOSE_VERSION = "2.30.3"
TIMEZONE = "Asia/Kolkata"

# Parse CLI arguments
git_local_dir = ARGV.include?("--gitlocal-dir") ? 
  ARGV[ARGV.index("--gitlocal-dir") + 1] : 
  "D:/Developpement/workspaces/payment-gateway"

# Validate host path exists
unless File.directory?(git_local_dir)
  puts "WARNING: Host path '#{git_local_dir}' does not exist. Using default."
  git_local_dir = "./"
end

Vagrant.configure("2") do |config|
  config.vm.box = "generic/rocky9"

  config.vm.hostname = VM_NAME

  # Private network with static IP
  config.vm.network "private_network", ip: VM_IP

  # Synced folder - host workspace
  config.vm.synced_folder git_local_dir, "/workspace", type: "virtualbox"

  # Provider configuration for VirtualBox
  config.vm.provider "virtualbox" do |vb|
    vb.name = VM_NAME
    vb.memory = VM_MEMORY
    vb.cpus = VM_CPUS
    vb.linked_clone = true
    vb.check_guest_additions = false
  end

  # Provision script
  config.vm.provision "shell", inline: <<-SHELL
    set -e
    
    echo "=== Starting VM Provisioning ===" 
    
    # Update and install dependencies
    echo "[1/12] Installing dependencies..."
    dnf update -y
    dnf install -y dnf-plugins-core device-mapper-persistent-data lvm2 git curl wget tar gzip
    
    # Install PostgreSQL client and other tools
    echo "[2/12] Installing PostgreSQL client and tools..."
    dnf install -y postgresql15 postgresql15-server postgresql15-libs
    dnf install -y vim nano htop tree net-tools
    
    # Add Docker repository
    echo "[3/12] Adding Docker repository..."
    dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    
    # Install Docker
    echo "[4/12] Installing Docker #{DOCKER_VERSION}..."
    dnf install -y docker-ce-#{DOCKER_VERSION} docker-ce-cli-#{DOCKER_VERSION} containerd.io docker-buildx-plugin docker-compose-plugin
    
    # Enable and start Docker
    echo "[5/12] Starting Docker service..."
    systemctl enable docker
    systemctl start docker
    
    # Configure Docker daemon
    echo "[6/12] Configuring Docker daemon..."
    mkdir -p /etc/docker
    cat > /etc/docker/daemon.json <<EOF
{
  "insecure-registries": [
    "#{VM_IP}:5000"
  ],
  "registry-mirrors": [],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF
    
    # Restart Docker to apply config
    echo "[7/12] Restarting Docker..."
    systemctl restart docker
    
    # Add vagrant user to docker group
    echo "[8/12] Adding vagrant to docker group..."
    usermod -aG docker vagrant
    
    # Fix time sync
    echo "[9/12] Configuring timezone and time sync..."
    timedatectl set-timezone #{TIMEZONE}
    systemctl enable chronyd
    systemctl start chronyd
    
    # Disable SELinux (for development ease)
    echo "[10/12] Configuring SELinux..."
    setenforce 0
    sed -i 's/SELINUX=enforcing/SELINUX=permissive/' /etc/selinux/config
    
    # Install Docker Compose standalone
    echo "[11/12] Installing Docker Compose standalone..."
    if ! command -v docker-compose &> /dev/null; then
      curl -L "https://github.com/docker/compose/releases/download/v#{COMPOSE_VERSION}/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
      chmod +x /usr/local/bin/docker-compose
    fi
    
    # Install additional dev tools
    echo "[12/12] Installing additional dev tools..."
    if ! command -v docker-credential-pass &> /dev/null; then
      dnf install -y pass gnupg2
    fi
    
    echo "=== VM Provisioning Complete ==="
    echo ""
    echo "Docker version: $(docker --version)"
    echo "Docker Compose: $(docker-compose --version 2>/dev/null || echo 'standalone not installed')"
    echo ""
    echo "Next steps:"
    echo "  1. vagrant ssh"
    echo "  2. cd /workspace"
    echo "  3. docker compose -f docker-compose.dev.yml up -d"
  SHELL

  # Print info after provisioning
  config.vm.post_up_message = <<-MSG
    ============================================
    Payment Gateway Dev VM Ready!
    ============================================
    IP: #{VM_IP}
    Memory: #{VM_MEMORY}MB
    CPUs: #{VM_CPUS}
    
    Access:
      vagrant ssh
      cd /workspace
    
    Docker status:
      docker ps
      docker-compose --version
    
    Start services:
      cd /workspace
      docker compose -f docker-compose.dev.yml up -d
    
    Dashboard:
      http://#{VM_IP}:3001
    Frontend:
      http://#{VM_IP}:3000
    API:
      http://#{VM_IP}:8080
  MSG
end
