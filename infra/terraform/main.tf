terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.49"
    }
  }
}

provider "hcloud" {
  token = var.hcloud_token
}

resource "hcloud_ssh_key" "mrgreen" {
  name       = "mrgreen"
  public_key = file(var.ssh_public_key_path)
}

resource "hcloud_firewall" "mrgreen" {
  name = "mrgreen"

  dynamic "rule" {
    for_each = var.allow_public_ssh ? [1] : []
    content {
      direction  = "in"
      protocol   = "tcp"
      port       = "22"
      source_ips = ["0.0.0.0/0", "::/0"]
    }
  }

  rule {
    direction       = "out"
    protocol        = "tcp"
    port            = "any"
    destination_ips = ["0.0.0.0/0", "::/0"]
  }

  rule {
    direction       = "out"
    protocol        = "udp"
    port            = "any"
    destination_ips = ["0.0.0.0/0", "::/0"]
  }
}

resource "hcloud_server" "mrgreen" {
  name         = "mrgreen"
  server_type  = "cax11"
  image        = "ubuntu-24.04"
  location     = "nbg1"
  ssh_keys     = [hcloud_ssh_key.mrgreen.id]
  firewall_ids = [hcloud_firewall.mrgreen.id]
}
