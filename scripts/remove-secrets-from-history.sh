#!/bin/bash

# 🔒 Remove Exposed Secrets from Git History
# This script removes files containing exposed secrets from git history

set -e

echo "🔒 Git History Secret Removal Script"
echo "===================================="
echo ""
echo "⚠️  WARNING: This script will rewrite git history!"
echo "⚠️  All team members will need to re-clone the repository!"
echo "⚠️  Make sure you have a backup before proceeding!"
echo ""

# Check if git-filter-repo is installed
if ! command -v git-filter-repo &> /dev/null; then
    echo "❌ git-filter-repo is not installed!"
    echo ""
    echo "📦 Install it with:"
    echo "   pip3 install git-filter-repo"
    echo "   # or"
    echo "   brew install git-filter-repo  # macOS"
    echo ""
    exit 1
fi

# Confirm with user
read -p "⚠️  Do you want to proceed with removing secrets from git history? (yes/no): " -r
echo
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "❌ Cancelled. No changes made."
    exit 0
fi

echo "📋 Creating backup..."
BACKUP_DIR="../EmploymentVC-backup-$(date +%Y%m%d-%H%M%S)"
cp -r . "$BACKUP_DIR"
echo "✅ Backup created at: $BACKUP_DIR"
echo ""

echo "🔍 Files to be removed from history:"
echo "  1. waltid-identity/waltid-services/waltid-verifier-api/src/main/kotlin/id/walt/verifier/entra/EntraVerifierApi.kt"
echo "  2. waltid-identity/waltid-libraries/protocols/waltid-openid4vc/src/jvmTest/kotlin/id/walt/oid4vc/VP_JVM_Test.kt"
echo "  3. waltid-identity/waltid-libraries/protocols/waltid-openid4vc/src/jvmTest/kotlin/id/walt/oid4vc/CI_JVM_Test.kt"
echo ""

read -p "Continue? (yes/no): " -r
echo
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "❌ Cancelled."
    exit 0
fi

echo "🧹 Removing secrets from git history..."
echo ""

# Remove EntraVerifierApi.kt
echo "📝 Removing EntraVerifierApi.kt..."
git filter-repo --path waltid-identity/waltid-services/waltid-verifier-api/src/main/kotlin/id/walt/verifier/entra/EntraVerifierApi.kt --invert-paths --force

# Remove VP_JVM_Test.kt
echo "📝 Removing VP_JVM_Test.kt..."
git filter-repo --path waltid-identity/waltid-libraries/protocols/waltid-openid4vc/src/jvmTest/kotlin/id/walt/oid4vc/VP_JVM_Test.kt --invert-paths --force

# Remove CI_JVM_Test.kt
echo "📝 Removing CI_JVM_Test.kt..."
git filter-repo --path waltid-identity/waltid-libraries/protocols/waltid-openid4vc/src/jvmTest/kotlin/id/walt/oid4vc/CI_JVM_Test.kt --invert-paths --force

echo ""
echo "✅ Secrets removed from git history!"
echo ""
echo "📋 Next Steps:"
echo ""
echo "1. ⚠️  ROTATE Azure AD client secret immediately:"
echo "   - Go to Azure Portal → App Registrations"
echo "   - Find app: e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0"
echo "   - Delete old secret, create new one"
echo "   - Update your application configuration"
echo ""
echo "2. 🔍 Verify the cleanup:"
echo "   git log --all --oneline | grep 945a7d99"
echo "   # Should show the commit is gone or rewritten"
echo ""
echo "3. 🚀 Force push to remote (coordinate with team!):"
echo "   git push origin --force --all"
echo "   git push origin --force --tags"
echo ""
echo "4. 👥 Notify team members to re-clone:"
echo "   rm -rf EmploymentVC"
echo "   git clone <repository-url>"
echo ""
echo "5. ✅ Mark GitHub security alerts as resolved"
echo ""
echo "⚠️  Remember: All team members must re-clone the repository!"
echo ""

