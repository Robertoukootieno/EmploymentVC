output "namespace_name" {
  description = "Kubernetes namespace name"
  value       = kubernetes_namespace.employmentvc.metadata[0].name
}

output "service_account_name" {
  description = "Service account name"
  value       = kubernetes_service_account.employmentvc.metadata[0].name
}

output "config_map_name" {
  description = "ConfigMap name"
  value       = kubernetes_config_map.employmentvc.metadata[0].name
}

output "secret_name" {
  description = "Secret name"
  value       = kubernetes_secret.employmentvc.metadata[0].name
}
