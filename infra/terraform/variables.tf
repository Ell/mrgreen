variable "hcloud_token" {
  description = "Hetzner Cloud API token"
  sensitive   = true
}

variable "ssh_public_key_path" {
  description = "Path to SSH public key"
  default     = "~/.ssh/id_ed25519.pub"
}

variable "allow_public_ssh" {
  description = "Allow SSH (port 22) inbound from the internet. Disable after Tailscale is configured."
  type        = bool
  default     = true
}
