# 🔐 **Azure AD Client Secret Rotation - Step-by-Step Guide**

## 🎯 **What We're Rotating**

**Exposed Secret**: `ctL8Q~Ezdrcrju85gEtvbCmQQDmm7bXjJKsdXbCr`  
**Application ID**: `e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0`  
**Tenant ID**: `8bc955d9-38fd-4c15-a520-0c656407537a`

---

## 📋 **Method 1: Azure Portal (Easiest - Recommended)**

### **Step 1: Log in to Azure Portal**

1. Open your browser and go to: **https://portal.azure.com**
2. Sign in with your Azure account (the one that has access to this application)

### **Step 2: Navigate to App Registrations**

1. In the Azure Portal search bar (top), type: **"App registrations"**
2. Click on **"App registrations"** from the results
3. You'll see a list of all registered applications

### **Step 3: Find Your Application**

**Option A - Search by App ID:**
1. In the search box, paste: `e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0`
2. Click on the application when it appears

**Option B - Browse the list:**
1. Look through the list for your application
2. Click on it to open

### **Step 4: Go to Certificates & Secrets**

1. In the left sidebar, click **"Certificates & secrets"**
2. Click on the **"Client secrets"** tab
3. You'll see a list of all client secrets for this app

### **Step 5: Create a NEW Secret (Before Deleting Old One)**

⚠️ **IMPORTANT**: Create the new secret FIRST, then test it, THEN delete the old one!

1. Click **"+ New client secret"** button
2. Fill in the form:
   - **Description**: `Rotated after GitHub exposure - Dec 2025`
   - **Expires**: Choose based on your security policy:
     - **6 months** (recommended for high security)
     - **12 months** (balanced)
     - **24 months** (less maintenance)
3. Click **"Add"**

### **Step 6: COPY THE NEW SECRET IMMEDIATELY**

🚨 **CRITICAL**: You can only see the secret value ONCE!

1. After clicking "Add", you'll see the new secret with its **Value** shown
2. Click the **copy icon** next to the Value
3. **PASTE IT SOMEWHERE SAFE IMMEDIATELY** (like a password manager or secure note)
4. The value will look like: `ctL8Q~XxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXx`

**Example:**
```
New Secret Value: ctL8Q~NewSecretValueHere123456789ABCDEFGH
Secret ID: 12345678-1234-1234-1234-123456789abc
```

### **Step 7: Update Your Application Configuration**

Now you need to update your application to use the new secret. Choose the method you're using:

#### **Option A: Environment Variables (Local Development)**

```bash
# Update your .env file (DO NOT COMMIT THIS!)
# Edit: .env.development or .env.local

AZURE_CLIENT_SECRET=ctL8Q~NewSecretValueHere123456789ABCDEFGH
AZURE_CLIENT_ID=e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0
AZURE_TENANT_ID=8bc955d9-38fd-4c15-a520-0c656407537a
```

#### **Option B: Kubernetes Secrets (Production)**

```bash
# Update Kubernetes secret
kubectl create secret generic azure-secrets \
  --from-literal=client-id=e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0 \
  --from-literal=client-secret=ctL8Q~NewSecretValueHere123456789ABCDEFGH \
  --from-literal=tenant-id=8bc955d9-38fd-4c15-a520-0c656407537a \
  --namespace=provenly \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart pods to pick up new secret
kubectl rollout restart deployment/verifier-api -n provenly
```

#### **Option C: Azure Key Vault (Best Practice)**

```bash
# Store in Azure Key Vault
az keyvault secret set \
  --vault-name your-keyvault-name \
  --name azure-client-secret \
  --value "ctL8Q~NewSecretValueHere123456789ABCDEFGH"

# Your application should read from Key Vault
# Update your app config to point to Key Vault
```

#### **Option D: Docker Environment Variables**

```bash
# Update docker-compose.yml or Dockerfile
# Edit the environment section:

environment:
  - AZURE_CLIENT_SECRET=ctL8Q~NewSecretValueHere123456789ABCDEFGH
  - AZURE_CLIENT_ID=e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0
  - AZURE_TENANT_ID=8bc955d9-38fd-4c15-a520-0c656407537a

# Restart containers
docker-compose down
docker-compose up -d
```

### **Step 8: Test the New Secret**

Before deleting the old secret, make sure the new one works!

```bash
# Test your application
# Try to authenticate with Azure AD
# Check logs for any authentication errors

# Example test (if you have a test endpoint):
curl -X POST https://your-app.com/api/test-azure-auth

# Check application logs
kubectl logs -f deployment/verifier-api -n provenly
# or
docker logs your-container-name
```

### **Step 9: Delete the OLD Secret**

⚠️ **Only do this AFTER confirming the new secret works!**

1. Go back to Azure Portal → App registrations → Your app → Certificates & secrets
2. Find the OLD secret (the one that was exposed)
   - Look for the description or creation date
   - The exposed secret starts with: `ctL8Q~Ezdrcrju85gEtvbCmQQDmm7bXjJKsdXbCr`
3. Click the **trash can icon** next to it
4. Confirm deletion

### **Step 10: Verify Everything Works**

```bash
# Test your application again
# Make sure all Azure AD integrations work
# Check for any authentication errors in logs
```

---

## 📋 **Method 2: Azure CLI (For Developers)**

If you prefer command line:

### **Prerequisites:**

```bash
# Install Azure CLI (if not already installed)
# macOS
brew install azure-cli

# Linux
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# Windows
# Download from: https://aka.ms/installazurecliwindows
```

### **Steps:**

```bash
# 1. Login to Azure
az login

# 2. Set the subscription (if you have multiple)
az account set --subscription "Your-Subscription-Name"

# 3. Create a new client secret
az ad app credential reset \
  --id e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0 \
  --append \
  --display-name "Rotated-Dec-2025" \
  --years 1

# This will output something like:
# {
#   "appId": "e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0",
#   "password": "ctL8Q~NewSecretValueHere123456789ABCDEFGH",
#   "tenant": "8bc955d9-38fd-4c15-a520-0c656407537a"
# }

# 4. COPY THE PASSWORD VALUE IMMEDIATELY!

# 5. Update your application configuration (see Method 1, Step 7)

# 6. Test the new secret

# 7. List all credentials to find the old one
az ad app credential list --id e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0

# 8. Delete the old credential (after testing!)
az ad app credential delete \
  --id e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0 \
  --key-id <OLD_KEY_ID_FROM_LIST>
```

---

## 📋 **Method 3: PowerShell (For Windows Admins)**

```powershell
# 1. Install Azure AD module
Install-Module -Name Az -AllowClobber -Scope CurrentUser

# 2. Connect to Azure
Connect-AzAccount

# 3. Create new client secret
$appId = "e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0"
$newSecret = New-AzADAppCredential -ApplicationId $appId -DisplayName "Rotated-Dec-2025"

# 4. Display the new secret (COPY THIS!)
Write-Host "New Secret: $($newSecret.SecretText)"

# 5. Update your application configuration

# 6. Test the new secret

# 7. Remove old credential (after testing!)
Remove-AzADAppCredential -ApplicationId $appId -KeyId <OLD_KEY_ID>
```

---

## ✅ **Verification Checklist**

After rotation, verify:

- [ ] ✅ New secret created in Azure Portal
- [ ] ✅ New secret copied and stored securely
- [ ] ✅ Application configuration updated
- [ ] ✅ Application restarted/redeployed
- [ ] ✅ Application tested and working
- [ ] ✅ Old secret deleted from Azure
- [ ] ✅ No authentication errors in logs
- [ ] ✅ All team members notified

---

## 🚨 **Common Issues & Solutions**

### **Issue 1: "I don't have access to Azure Portal"**
**Solution**: Contact your Azure administrator or IT team

### **Issue 2: "I can't find the application"**
**Solution**: 
- Make sure you're in the correct tenant
- Check if you have the right permissions
- Ask your Azure admin for access

### **Issue 3: "Application still uses old secret after update"**
**Solution**:
- Restart your application/containers
- Clear application cache
- Check environment variables are loaded correctly

### **Issue 4: "I forgot to copy the new secret"**
**Solution**:
- Delete the secret you just created
- Create a new one and copy it this time

---

## 📞 **Need Help?**

If you encounter issues:
1. Check Azure AD audit logs for errors
2. Contact your Azure administrator
3. Review application logs for authentication errors

---

**Status**: Ready to execute  
**Time Required**: 10-15 minutes  
**Difficulty**: Easy (Portal) / Medium (CLI)


