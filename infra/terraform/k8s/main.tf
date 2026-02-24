terraform {
  required_version = ">= 1.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.28"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
  }

  backend "local" {
    path = "terraform/state/kubernetes.tfstate"
  }
}

provider "kubernetes" {
  config_path    = var.kubeconfig_path
  config_context = var.k8s_context
}

provider "helm" {
  kubernetes {
    config_path    = var.kubeconfig_path
    config_context = var.k8s_context
  }
}

# Create project namespace
resource "kubernetes_namespace" "employmentvc" {
  metadata {
    name = var.namespace
    labels = {
      "app.kubernetes.io/name"     = "employmentvc"
      "app.kubernetes.io/part-of"  = "employment-vc-platform"
      "environment"                 = var.environment
    }
  }
}

# Create service account
resource "kubernetes_service_account" "employmentvc" {
  metadata {
    name      = "employmentvc-sa"
    namespace = kubernetes_namespace.employmentvc.metadata[0].name
  }
}

# Create RBAC ClusterRole
resource "kubernetes_cluster_role" "employmentvc" {
  metadata {
    name = "employmentvc-role"
  }

  rule {
    api_groups = [""]
    resources  = ["configmaps", "secrets", "services", "endpoints"]
    verbs      = ["get", "list", "watch"]
  }

  rule {
    api_groups = ["apps"]
    resources  = ["deployments", "statefulsets"]
    verbs      = ["get", "list", "watch"]
  }
}

# Bind role to service account
resource "kubernetes_cluster_role_binding" "employmentvc" {
  metadata {
    name = "employmentvc-rolebinding"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = kubernetes_cluster_role.employmentvc.metadata[0].name
  }

  subject {
    kind      = "ServiceAccount"
    name      = kubernetes_service_account.employmentvc.metadata[0].name
    namespace = kubernetes_namespace.employmentvc.metadata[0].name
  }
}

# Create network policy
resource "kubernetes_network_policy" "employmentvc" {
  metadata {
    name      = "employmentvc-network-policy"
    namespace = kubernetes_namespace.employmentvc.metadata[0].name
  }

  spec {
    pod_selector {
      match_labels = {
        "app.kubernetes.io/part-of" = "employment-vc-platform"
      }
    }

    policy_type = ["Ingress", "Egress"]

    ingress {
      from {
        namespace_selector {
          match_labels = {
            "name" = kubernetes_namespace.employmentvc.metadata[0].name
          }
        }
      }
    }

    egress {
      to {
        namespace_selector {}
      }
      ports {
        protocol = "TCP"
        port     = "443"
      }
      ports {
        protocol = "TCP"
        port     = "80"
      }
    }

    # Allow DNS
    egress {
      to {
        namespace_selector {}
      }
      ports {
        protocol = "UDP"
        port     = "53"
      }
    }
  }
}

# ConfigMap for application configuration
resource "kubernetes_config_map" "employmentvc" {
  metadata {
    name      = "employmentvc-config"
    namespace = kubernetes_namespace.employmentvc.metadata[0].name
  }

  data = {
    SPRING_PROFILES_ACTIVE = var.environment
    NODE_ENV               = var.environment
    LOG_LEVEL              = var.log_level
    POSTGRES_HOST          = var.postgres_host
    POSTGRES_PORT          = var.postgres_port
    REDIS_HOST             = var.redis_host
    REDIS_PORT             = var.redis_port
  }
}

# Secret for sensitive data
resource "kubernetes_secret" "employmentvc" {
  metadata {
    name      = "employmentvc-secrets"
    namespace = kubernetes_namespace.employmentvc.metadata[0].name
  }

  type = "Opaque"

  data = {
    POSTGRES_PASSWORD = base64encode(var.postgres_password)
    REDIS_PASSWORD    = base64encode(var.redis_password)
  }

  sensitive = true
}
