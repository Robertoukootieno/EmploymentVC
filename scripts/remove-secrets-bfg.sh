#!/bin/bash

# 🔒 Remove Exposed Secrets using BFG Repo-Cleaner
# Safer and faster alternative to git-filter-repo

set -e

echo "🔒 BFG Repo-Cleaner - Secret Removal Script"
echo "==========================================="
echo ""

# Check if BFG is installed
if ! command -v bfg &> /dev/null; then
    echo "❌ BFG Repo-Cleaner is not installed!"
    echo ""
    echo "📦 Install it with:"
    echo "   brew install bfg  # macOS"
    echo "   # or download from: https://rtyley.github.io/bfg-repo-cleaner/"
    echo ""
    exit 1
fi

echo "📋 This script will remove the following secrets:"
echo "  • Azure AD Client Secret: ctL8Q~Ezdrcrju85gEtvbCmQQDmm7bXjJKsdXbCr"
echo "  • Expired JWT tokens from test files"
echo ""
echo "⚠️  WARNING: This rewrites git history!"
echo "⚠️  Team members will need to re-clone!"
echo ""

read -p "Continue? (yes/no): " -r
echo
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "❌ Cancelled."
    exit 0
fi

# Create secrets file
echo "📝 Creating secrets replacement file..."
cat > /tmp/secrets-to-remove.txt << 'EOF'
ctL8Q~Ezdrcrju85gEtvbCmQQDmm7bXjJKsdXbCr
eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlQxU3QtZExUdnlXUmd4Ql82NzZ1OGtyWFMtSSJ9
EOF

echo "✅ Secrets file created"
echo ""

# Create backup
echo "📋 Creating backup..."
BACKUP_DIR="../EmploymentVC-backup-$(date +%Y%m%d-%H%M%S)"
cp -r . "$BACKUP_DIR"
echo "✅ Backup created at: $BACKUP_DIR"
echo ""

# Run BFG
echo "🧹 Running BFG Repo-Cleaner..."
bfg --replace-text /tmp/secrets-to-remove.txt

echo ""
echo "🧹 Cleaning up repository..."
git reflog expire --expire=now --all
git gc --prune=now --aggressive

echo ""
echo "✅ Secrets removed from git history!"
echo ""
echo "📋 Next Steps:"
echo ""
echo "1. ⚠️  ROTATE Azure AD client secret IMMEDIATELY"
echo "2. 🔍 Verify: git log --all --grep='945a7d99'"
echo "3. 🚀 Force push: git push origin --force --all"
echo "4. 👥 Notify team to re-clone"
echo "5. ✅ Mark GitHub alerts as resolved"
echo ""
echo "🗑️  Cleanup:"
rm -f /tmp/secrets-to-remove.txt
echo "   Temporary files removed"
echo ""

