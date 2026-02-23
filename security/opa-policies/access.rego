# Access Control Policy for EmploymentVC
# Controls API access and RBAC

package access

import future.keywords.if
import future.keywords.in

# Default deny all access
default allow = false

# Main access control rule
allow if {
    user_authenticated
    has_required_role
    has_required_permission
    not user_blocked
    within_rate_limits
}

# Authentication check
user_authenticated if {
    input.user.authenticated == true
    token_valid
}

token_valid if {
    input.user.token
    input.user.token_expiry
    time.parse_rfc3339_ns(input.user.token_expiry) > time.now_ns()
}

# Role-based access control
has_required_role if {
    required_roles := role_requirements[input.request.path][input.request.method]
    user_role := input.user.role
    user_role in required_roles
}

# Permission-based access control
has_required_permission if {
    required_perms := permission_requirements[input.request.path][input.request.method]
    user_permissions := {perm | perm := input.user.permissions[_]}
    required_permission_set := {perm | perm := required_perms[_]}
    
    # User must have all required permissions
    count(required_permission_set - user_permissions) == 0
}

# Role requirements mapping
role_requirements := {
    "/api/v1/issuer/credentials": {
        "POST": ["employer", "admin"],
        "GET": ["employer", "admin", "auditor"],
        "DELETE": ["admin"]
    },
    "/api/v1/verifier/verify": {
        "POST": ["employer", "verifier", "admin"]
    },
    "/api/v1/wallet/credentials": {
        "GET": ["employee", "employer", "admin"],
        "POST": ["employee", "admin"],
        "DELETE": ["employee", "admin"]
    },
    "/api/v1/did/register": {
        "POST": ["employee", "employer", "admin"]
    },
    "/api/v1/schemas": {
        "POST": ["admin"],
        "GET": ["employee", "employer", "verifier", "admin"],
        "PUT": ["admin"],
        "DELETE": ["admin"]
    },
    "/api/v1/workflow/employment": {
        "POST": ["employer", "admin"],
        "GET": ["employee", "employer", "admin"]
    }
}

# Permission requirements mapping
permission_requirements := {
    "/api/v1/issuer/credentials": {
        "POST": ["issue:credential"],
        "DELETE": ["revoke:credential"]
    },
    "/api/v1/verifier/verify": {
        "POST": ["verify:credential"]
    },
    "/api/v1/wallet/credentials": {
        "POST": ["manage:wallet"],
        "DELETE": ["manage:wallet"]
    },
    "/api/v1/did/register": {
        "POST": ["register:did"]
    },
    "/api/v1/schemas": {
        "POST": ["manage:schema"],
        "PUT": ["manage:schema"],
        "DELETE": ["manage:schema"]
    }
}

# Check if user is blocked
user_blocked if {
    blocked_user := data.blocked_users[_]
    blocked_user.id == input.user.id
    time.parse_rfc3339_ns(blocked_user.until) > time.now_ns()
}

# Rate limiting checks
within_rate_limits if {
    not api_rate_limit_exceeded
    not user_rate_limit_exceeded
}

api_rate_limit_exceeded if {
    endpoint := input.request.path
    method := input.request.method
    
    requests := count([r | r := data.recent_requests[endpoint][method][_]])
    limit := data.rate_limits[endpoint][method]
    
    requests >= limit
}

user_rate_limit_exceeded if {
    user_requests := count(data.user_requests[input.user.id])
    user_requests > 1000  # 1000 requests per hour per user
}

# Admin-only operations
admin_only if {
    input.user.role == "admin"
    input.user.admin_verified == true
}

# Resource ownership check
owns_resource if {
    input.resource.owner_id == input.user.id
}

# Delegation check - allows users to perform actions on behalf of others
valid_delegation if {
    delegation := data.delegations[input.user.id]
    delegation.target_user == input.resource.owner_id
    delegation.permissions[_] == input.request.permission
    time.parse_rfc3339_ns(delegation.expires) > time.now_ns()
}

# Special access rules
allow if {
    admin_only
}

allow if {
    owns_resource
    user_authenticated
}

allow if {
    valid_delegation
    user_authenticated
}

# Audit logging requirement
requires_audit if {
    input.request.method in ["POST", "PUT", "DELETE"]
}

requires_audit if {
    input.request.path contains "/admin/"
}

requires_audit if {
    input.user.role == "admin"
}

# Deny with detailed reasons
deny_reason[msg] {
    not user_authenticated
    msg := "User is not authenticated or token is expired"
}

deny_reason[msg] {
    not has_required_role
    msg := sprintf("User role '%v' does not have access to %v %v", [input.user.role, input.request.method, input.request.path])
}

deny_reason[msg] {
    not has_required_permission
    msg := "User does not have required permissions"
}

deny_reason[msg] {
    user_blocked
    msg := "User account is temporarily blocked"
}

deny_reason[msg] {
    api_rate_limit_exceeded
    msg := "API rate limit exceeded for this endpoint"
}

deny_reason[msg] {
    user_rate_limit_exceeded
    msg := "User rate limit exceeded"
}
