# 🔒 **Step-by-Step Security Fix Guide**

## 🎯 **Quick Summary**

You have **4 exposed secrets** in your git history. Here's what to do:

| Priority | Action | Time Required |
|----------|--------|---------------|
| 🔴 **URGENT** | Rotate Azure AD secret | 10 minutes |
| 🟡 **HIGH** | Remove from git history | 30 minutes |
| 🟢 **MEDIUM** | Update security practices | 1 hour |

---

## 🚨 **STEP 1: Rotate Azure AD Client Secret (DO THIS FIRST!)**

### **Why This is Urgent:**
The exposed secret `ctL8Q~Ezdrcrju85gEtvbCmQQDmm7bXjJKsdXbCr` gives anyone access to your Azure AD application.

### **How to Rotate:**

```bash
# Option 1: Azure Portal (Recommended)
1. Go to: https://portal.azure.com
2. Navigate to: Azure Active Directory → App Registrations
3. Search for app ID: e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0
4. Click: Certificates & secrets
5. Find the exposed secret and click "Delete"
6. Click "New client secret"
7. Description: "Rotated after GitHub exposure - Dec 2025"
8. Expires: 6 months (or per your policy)
9. Click "Add"
10. **COPY THE NEW SECRET IMMEDIATELY** (you can't see it again!)

# Option 2: Azure CLI
az ad app credential reset \
  --id e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0 \
  --append \
  --display-name "Rotated-Dec-2025"
```

### **Update Your Application:**

```bash
# Update environment variables (DO NOT commit!)
export AZURE_CLIENT_SECRET="<NEW_SECRET_HERE>"

# Or update Kubernetes secret
kubectl create secret generic azure-secrets \
  --from-literal=client-secret=<NEW_SECRET> \
  --namespace=provenly \
  --dry-run=client -o yaml | kubectl apply -f -

# Or update Azure Key Vault
az keyvault secret set \
  --vault-name your-keyvault \
  --name azure-client-secret \
  --value "<NEW_SECRET>"
```

### **Test the New Secret:**

```bash
# Test that your application still works with the new secret
# Run your application and verify Azure AD authentication works
```

### **Delete the Old Secret:**

```bash
# After confirming the new secret works, delete the old one
# In Azure Portal: Certificates & secrets → Delete old secret
```

---

## 🧹 **STEP 2: Remove Secrets from Git History**

### **Option A: Using GitHub's Secret Scanning (Easiest)**

GitHub can automatically remove secrets for you:

1. Go to your repository on GitHub
2. Click: Settings → Security → Secret scanning alerts
3. For each alert, click "Dismiss" → "Revoked"
4. GitHub will help you remove them from history

### **Option B: Manual Removal (More Control)**

#### **Install git-filter-repo:**

```bash
# macOS
brew install git-filter-repo

# Linux
pip3 install git-filter-repo

# Or download from: https://github.com/newren/git-filter-repo
```

#### **Run the Removal Script:**

```bash
cd /home/robert/EmploymentVC

# Make script executable
chmod +x scripts/remove-secrets-from-history.sh

# Run it (creates backup automatically)
./scripts/remove-secrets-from-history.sh
```

#### **Or Manual Commands:**

```bash
# Create backup first!
cp -r /home/robert/EmploymentVC /home/robert/EmploymentVC-backup

cd /home/robert/EmploymentVC

# Remove the files with secrets
git filter-repo \
  --path waltid-identity/waltid-services/waltid-verifier-api/src/main/kotlin/id/walt/verifier/entra/EntraVerifierApi.kt \
  --path waltid-identity/waltid-libraries/protocols/waltid-openid4vc/src/jvmTest/kotlin/id/walt/oid4vc/VP_JVM_Test.kt \
  --path waltid-identity/waltid-libraries/protocols/waltid-openid4vc/src/jvmTest/kotlin/id/walt/oid4vc/CI_JVM_Test.kt \
  --invert-paths \
  --force

# Clean up
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

#### **Force Push (Coordinate with Team!):**

```bash
# ⚠️  WARNING: This will rewrite history for everyone!
# Make sure team is ready to re-clone!

git push origin --force --all
git push origin --force --tags
```

#### **Notify Team Members:**

Send this message to your team:

```
🚨 URGENT: Git history has been rewritten to remove exposed secrets.

Please do the following IMMEDIATELY:

1. Commit and push any local changes
2. Delete your local repository:
   rm -rf EmploymentVC

3. Re-clone the repository:
   git clone <repository-url>

4. Reinstall dependencies:
   cd EmploymentVC
   npm install  # or your build command

DO NOT try to pull or merge - you must re-clone!
```

---

## 🛡️ **STEP 3: Prevent Future Leaks**

### **Update .gitignore:**

```bash
cd /home/robert/EmploymentVC

# Add secret patterns to .gitignore
cat >> .gitignore << 'EOF'

# Secrets and credentials
*.secret
*.key
*.pem
!**/test/**/*.pem
.env.local
.env.production
.env.*.local
**/secrets/
**/*secret*.txt
**/*password*.txt
**/*token*.txt

# Azure credentials
azure-credentials.json
service-principal.json

# AWS credentials
.aws/credentials

# Private keys
id_rsa
id_ed25519
*.p12
*.pfx

EOF

git add .gitignore
git commit -m "security: Update .gitignore to prevent secret leaks"
git push
```

### **Install Pre-commit Hooks:**

```bash
# Install git-secrets
brew install git-secrets  # macOS
# or
pip install git-secrets

# Set up for this repo
cd /home/robert/EmploymentVC
git secrets --install
git secrets --register-aws

# Add custom patterns
git secrets --add 'ctL8Q~[A-Za-z0-9_-]+'  # Azure client secrets
git secrets --add 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+'  # JWT
git secrets --add 'AKIA[0-9A-Z]{16}'  # AWS keys
git secrets --add 'sk-[a-zA-Z0-9]{48}'  # OpenAI keys

# Test it
echo "ctL8Q~test" > test-secret.txt
git add test-secret.txt
git commit -m "test"  # Should fail!
rm test-secret.txt
```

---

## ✅ **Verification Checklist**

After completing all steps:

- [ ] ✅ Azure AD client secret rotated
- [ ] ✅ New secret tested and working
- [ ] ✅ Old secret deleted from Azure
- [ ] ✅ Secrets removed from git history
- [ ] ✅ Force pushed to remote
- [ ] ✅ Team notified to re-clone
- [ ] ✅ .gitignore updated
- [ ] ✅ Pre-commit hooks installed
- [ ] ✅ GitHub security alerts dismissed
- [ ] ✅ No secrets in current codebase: `git secrets --scan`

---

## 📞 **Need Help?**

If you encounter issues:

1. **Azure secret rotation fails**: Contact Azure admin
2. **Git history rewrite fails**: Restore from backup
3. **Team coordination issues**: Use a maintenance window
4. **GitHub alerts persist**: Wait 24 hours or contact GitHub support

---

**Status**: ⚠️ **ACTION REQUIRED**  
**Estimated Time**: 1-2 hours  
**Impact**: High (requires team coordination)


