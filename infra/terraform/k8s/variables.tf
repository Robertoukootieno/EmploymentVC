variable "kubeconfig_path" {
  description = "Path to kubeconfig file"
  type        = string
  default     = "~/.kube/config"
}

variable "k8s_context" {
  description = "Kubernetes context to use"
  type        = string
  default     = "docker-desktop"
}

variable "namespace" {
  description = "Kubernetes namespace for EmploymentVC"
  type        = string
  default     = "provenly"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "development"
  
  validation {
    condition     = contains(["development", "staging", "production"], var.environment)
    error_message = "Environment must be development, staging, or production."
  }
}

variable "log_level" {
  description = "Application log level"
  type        = string
  default     = "info"
  
  validation {
    condition     = contains(["debug", "info", "warn", "error"], var.log_level)
    error_message = "Log level must be debug, info, warn, or error."
  }
}

variable "postgres_host" {
  description = "PostgreSQL host"
  type        = string
  default     = "postgres-service"
}

variable "postgres_port" {
  description = "PostgreSQL port"
  type        = number
  default     = 5432
}

variable "postgres_password" {
  description = "PostgreSQL password"
  type        = string
  sensitive   = true
}

variable "redis_host" {
  description = "Redis host"
  type        = string
  default     = "redis-service"
}

variable "redis_port" {
  description = "Redis port"
  type        = number
  default     = 6379
}

variable "redis_password" {
  description = "Redis password"
  type        = string
  sensitive   = true
}
