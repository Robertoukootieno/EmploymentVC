# 🚀 **Quick Azure Secret Rotation - 5 Minutes**

## 🎯 **Super Simple Steps**

### **1. Open Azure Portal** (1 minute)
```
👉 Go to: https://portal.azure.com
👉 Sign in with your Azure account
```

### **2. Find Your App** (1 minute)
```
👉 Search bar (top) → type "App registrations"
👉 Click "App registrations"
👉 Search for: e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0
👉 Click on the app
```

### **3. Create New Secret** (2 minutes)
```
👉 Left sidebar → "Certificates & secrets"
👉 Click "Client secrets" tab
👉 Click "+ New client secret" button
👉 Description: "Rotated Dec 2025"
👉 Expires: Choose "6 months"
👉 Click "Add"
```

### **4. COPY THE SECRET!** (30 seconds)
```
🚨 CRITICAL: You can only see this ONCE!

👉 Click the COPY icon next to "Value"
👉 Paste it somewhere safe RIGHT NOW
👉 It looks like: ctL8Q~XxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXx
```

### **5. Update Your App** (1 minute)
```
👉 Find where your app stores this secret
👉 Replace the old secret with the new one
👉 Restart your app
```

### **6. Test & Delete Old Secret** (30 seconds)
```
👉 Test your app works
👉 Go back to Azure Portal
👉 Delete the old secret (trash icon)
```

---

## 📸 **Visual Guide**

### **What You'll See in Azure Portal:**

```
┌─────────────────────────────────────────────────────┐
│ Azure Portal                                         │
├─────────────────────────────────────────────────────┤
│                                                      │
│  App registrations                                   │
│  ├─ All applications                                 │
│  └─ [Search: e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0]  │
│                                                      │
│  Your App Name                                       │
│  ├─ Overview                                         │
│  ├─ Authentication                                   │
│  ├─ Certificates & secrets  ← CLICK HERE            │
│  │   ├─ Certificates                                │
│  │   └─ Client secrets                              │
│  │       ├─ [+ New client secret]  ← CLICK HERE     │
│  │       ├─ Old Secret (exposed) ← DELETE LATER     │
│  │       └─ New Secret (just created) ← COPY THIS!  │
│  └─ API permissions                                  │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## 🔑 **Where to Update the Secret**

### **Check These Locations:**

1. **Environment Variables**
   ```bash
   # File: .env.development or .env.local
   AZURE_CLIENT_SECRET=<NEW_SECRET_HERE>
   ```

2. **Kubernetes Secrets**
   ```bash
   kubectl edit secret azure-secrets -n provenly
   # Update the client-secret field
   ```

3. **Docker Compose**
   ```yaml
   # File: docker-compose.yml
   environment:
     - AZURE_CLIENT_SECRET=<NEW_SECRET_HERE>
   ```

4. **Application Config**
   ```bash
   # Check these files:
   - backend-services/verifier-api/application.yml
   - .env.development
   - k8s/secrets.yaml
   ```

---

## 🎬 **Complete Example**

### **Before (Exposed Secret):**
```bash
AZURE_CLIENT_ID=e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0
AZURE_CLIENT_SECRET=ctL8Q~Ezdrcrju85gEtvbCmQQDmm7bXjJKsdXbCr  ← EXPOSED!
AZURE_TENANT_ID=8bc955d9-38fd-4c15-a520-0c656407537a
```

### **After (New Secret):**
```bash
AZURE_CLIENT_ID=e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0
AZURE_CLIENT_SECRET=ctL8Q~NewSecretHere123456789ABCDEFGHIJK  ← NEW & SAFE!
AZURE_TENANT_ID=8bc955d9-38fd-4c15-a520-0c656407537a
```

---

## ⚡ **Quick Commands**

### **If Using Kubernetes:**
```bash
# Update secret
kubectl create secret generic azure-secrets \
  --from-literal=client-secret=<NEW_SECRET> \
  --namespace=provenly \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart app
kubectl rollout restart deployment/verifier-api -n provenly
```

### **If Using Docker:**
```bash
# Update .env file
echo "AZURE_CLIENT_SECRET=<NEW_SECRET>" > .env.local

# Restart containers
docker-compose restart
```

### **If Using Azure CLI:**
```bash
# One command to rotate
az ad app credential reset \
  --id e50ceaa6-8554-4ae6-bfdf-fd95e2243ae0 \
  --append \
  --display-name "Rotated-Dec-2025"

# Copy the "password" value from output!
```

---

## ✅ **Done! Now What?**

After rotating the secret:

1. ✅ **Test your application** - Make sure it still works
2. ✅ **Delete old secret** - Remove it from Azure Portal
3. ✅ **Remove from git history** - Run the cleanup script
4. ✅ **Update .gitignore** - Prevent future leaks

---

## 🆘 **Stuck? Common Issues**

### **"I can't access Azure Portal"**
→ Ask your Azure admin for access

### **"I can't find the app"**
→ Make sure you're logged into the correct Azure tenant

### **"I forgot to copy the secret"**
→ Delete it and create a new one

### **"App doesn't work after rotation"**
→ Check you updated the secret in the right place
→ Restart your application

---

## 📞 **Get Help**

- **Azure Portal Help**: Click the "?" icon in top right
- **Azure Support**: https://portal.azure.com/#blade/Microsoft_Azure_Support/HelpAndSupportBlade
- **Documentation**: https://docs.microsoft.com/azure/active-directory/

---

**Time Required**: 5 minutes  
**Difficulty**: ⭐ Easy  
**Impact**: 🔴 Critical (do this first!)


