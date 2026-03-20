.PHONY: ansible-vault ansible-apply

ansible-vault:
	EDITOR=nvim ansible-vault edit infra/ansible/group_vars/all/vault.yml

ansible-apply:
	ansible-playbook infra/ansible/playbook.yml -i infra/ansible/inventory.yml --ask-vault-pass
